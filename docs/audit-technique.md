# Audit technique du projet SquareGames

> Document destiné à une IA ou un développeur futur pour comprendre l'état actuel du projet,
> ses forces, ses faiblesses, et les améliorations prioritaires à réaliser.
>
> Dernière mise à jour : 05/06/2026

---

## Vue d'ensemble

- **Langage** : Java 21+ / Spring Boot 3.5
- **Architecture** : Microservices (2 apps) + couches par feature (api/application/domain/infrastructure)
- **Persistance** : JPA (Hibernate) + H2 fichier (dev) / MySQL (profil production)
- **Tests** : 60 tests (api) + 14 tests (user-api) — Golden Master
- **Sécurité** : Spring Security + JWT (itération 5) — authentification stateless, BCrypt, rôles
- **Moteur de jeu** : bibliothèque externe `square-games-engine` (JAR fourni, code source non modifiable)

---

## Points forts

### 1. Architecture en couches par feature

Chaque feature (`game/`, `user/`) est découpée en 4 couches :

```
game/
├── api/            ← Contrôleurs REST + DTO (entrée/sortie HTTP)
├── application/    ← Services métier + interfaces (GameService, GameDao, GamePlugin)
├── domain/         ← Entités JPA + Repository Spring Data
└── infrastructure/ ← Implémentations techniques (JpaGameDao, JdbcGameDao, InMemoryGameDao)
```

**Pourquoi c'est bien** :
- La couche `application` ne dépend que d'interfaces, pas d'implémentations
- Changer de DAO (InMemory → JPA) n'impacte pas le service
- Ajouter un nouveau jeu = ajouter un `GamePlugin`, pas de modification du contrôleur

### 2. Pattern DAO avec implémentations multiples

3 implémentations coexistent :
- `InMemoryGameDao` — pour comprendre le pattern, ne survit pas au redémarrage
- `JdbcGameDao` — SQL explicite, limitation de reconstruction d'état
- `JpaGameDao` — **@Primary**, persistance complète via `createGameWithIds`

L'interface `GameDao` (dans `application/`) fait le pont :
```java
public interface GameDao {
    Collection<Game> findAll();
    Optional<Game> findById(UUID gameId);
    Game upsert(Game game);
    void delete(UUID gameId);
    Collection<Game> findByPlayerId(String playerId);
}
```

### 3. Persistance complète des parties

`JpaGameDao.convertToGame` utilise `GameFactory.createGameWithIds` pour reconstruire
l'état complet d'une partie depuis la base de données :
- `playerIds` restaurés depuis la colonne `player_ids`
- Tokens sur le plateau (`isOnBoard=true`) avec leurs positions
- Tokens retirés (`isRemoved=true`)
- Tokens restants (`isOnBoard=false, isRemoved=false`)

`GameServiceImpl.playMove` appelle `gameDao.upsert(game)` après chaque coup — sans cet appel,
les modifications en mémoire seraient perdues avec JPA (contrairement à InMemoryGameDao
où l'objet est stocké par référence).

### 4. Tests Golden Master

60 tests couvrant :
- Intégration API complète (CRUD, coups, codes HTTP, partie TicTacToe entière)
- Contrat inter-services avec WireMock
- Tests unitaires du service avec Mockito
- Tests JPA ciblés avec `@DataJpaTest`

Ces tests protègent contre les régressions lors des refactorings (DAO, JPA, etc.).

### 5. Profils Spring

- `h2` : H2 fichier (`jdbc:h2:file:./data/squaregames`) — données persistantes en dev
- `mysql` : MySQL — pour la production
- Tests : H2 mémoire (`jdbc:h2:mem:testdb`) avec `ddl-auto=create-drop`

---

## Points faibles et améliorations prioritaires

### 🔴 Priorité haute — Problèmes fonctionnels

#### P1. ✅ CORRIGÉ — user-api en H2 fichier

**Fichier** : `/home/user/Documents/JavaSpring_Project/user-api/src/main/resources/application.properties`

**Problème (corrigé)** : L'app de jeux persiste ses données (H2 fichier), mais user-api utilisait H2 en mémoire.
Après un redémarrage, les parties existaient toujours mais les utilisateurs n'existaient plus.
Quand un joueur essayait de jouer sur une partie restaurée → `403 Forbidden` car user-api ne reconnaissait plus l'utilisateur.

**Correction appliquée** (29/05/2026) :
```properties
# Avant :
spring.datasource.url=jdbc:h2:mem:userdb
# Après :
spring.datasource.url=jdbc:h2:file:./data/userdb
```

Le dossier `user-api/data/` a été ajouté au `.gitignore` (racine) pour ne pas commiter la base.
Fichier H2 créé : `user-api/data/userdb.mv.db` — les utilisateurs survivent maintenant au redémarrage.

---

#### P2. `findByPlayerId` charge toutes les parties en mémoire

**Fichier** : `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/JpaGameDao.java:184-196`

**Code actuel** :
```java
public Collection<Game> findByPlayerId(String playerId) {
    List<Game> games = new ArrayList<>();
    for (GameEntity entity : repository.findAll()) {        // ← charge TOUT
        if (entity.playerIds != null && entity.playerIds.contains(playerId)) {
            Game game = convertToGame(entity);               // ← reconstruit chaque jeu
            if (game != null) games.add(game);
        }
    }
    return games;
}
```

**Problème** : Charge toutes les parties de la base, les convertit toutes en objets `Game`,
puis filtre en Java. Avec 100 parties ça va, avec 10 000 ça ralentit considérablement.

**Solution** : Ajouter une requête JPQL dans `GameEntityRepository` :
```java
@Query("SELECT g FROM GameEntity g WHERE g.playerIds LIKE CONCAT('%', :playerId, '%')")
List<GameEntity> findByPlayerId(@Param("playerId") String playerId);
```
Puis dans `JpaGameDao.findByPlayerId`, utiliser cette requête au lieu de `findAll()`.

**Note** : Le `LIKE '%uuid%'` est un filtre approximatif (pourrait matcher un sous-UUID).
La solution idéale est une table de liaison (voir P5), mais JPQL est un fix intermédiaire rapide.

**Complexité** : Faible.

---

#### P3. Absence de `@Transactional` sur les opérations du service

**Fichier** : `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/game/application/GameServiceImpl.java`

**Problème** : `playMove` fait `token.moveTo()` + `gameDao.upsert(game)`. Si le `upsert` échoue
(ex: contrainte DB violée), l'état du Game en mémoire est modifié mais pas persisté → incohérence.
De même, `createGame` fait `plugin.getFactory().createGame()` + `gameDao.upsert(game)` sans transaction.

**Solution** : Ajouter `@Transactional` sur les méthodes du service qui modifient des données :
```java
@Transactional
public GameDto createGame(GameCreationParams params, String userId) { ... }

@Transactional
public GameDto playMove(UUID gameId, MoveRequest move, String userId) { ... }
```

**Prérequis** : Ajouter `spring-boot-starter-data-jpa` (déjà présent) et vérifier que
`JpaGameDao` est bien dans un contexte Spring (il l'est via `@Repository`).

**Complexité** : Faible — 2 annotations à ajouter.

---

### 🟡 Priorité moyenne — Dettes techniques

#### P4. Entités JPA avec champs publics

**Fichiers** :
- `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/game/domain/GameEntity.java`
- `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/game/domain/GameTokenEntity.java`

**Problème** : Tous les champs sont `public` sans accesseurs (getters/setters) :
```java
public String id;           // ← n'importe qui peut faire entity.id = "toto"
public String factoryId;
public int boardSize;
```

**Conséquences** :
- Pas d'encapsulation → pas de validation au niveau de l'entité
- Pas de point d'interception pour ajouter de la logique (logging, calcul dérivé)
- Difficile à refactorer quand d'autres classes accèdent directement aux champs

**Solution** : Passer les champs en `private` + générer getters/setters.
Attention : il faut aussi mettre à jour `JpaGameDao.convertToEntity` et `convertToGame`
qui accèdent directement aux champs.

**Complexité** : Moyenne — beaucoup de changements mécaniques mais pas de logique à repenser.

---

#### P5. `playerIds` sérialisé en String concaténée

**Fichier** : `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/JpaGameDao.java:136-138`

**Code actuel** :
```java
entity.playerIds = game.getPlayerIds().stream()
        .map(Object::toString)
        .collect(Collectors.joining(","));   // ← "uuid1,uuid2,uuid3"
```

**Problèmes** :
- Recherche par joueur = `LIKE '%uuid%'` — approximatif, peut matcher un sous-UUID
- Pas de contrainte d'intégrité référentielle (pas de clé étrangère vers users)
- Limité en taille (`VARCHAR(1000)`) — ok pour 2-3 joueurs, pas pour un jeu à 50 joueurs
- Le parsing inverse (`split(",")`) est fragile

**Solution idéale** : Créer une table de liaison `game_players` :
```sql
CREATE TABLE game_players (
    game_id VARCHAR(36) NOT NULL,
    player_id VARCHAR(36) NOT NULL,
    player_order INT NOT NULL,
    PRIMARY KEY (game_id, player_order),
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);
```
Avec une entité JPA `GamePlayerEntity` et une relation `@OneToMany` sur `GameEntity`.

**Complexité** : Moyenne — nouvelle table + entité + refactoring de convertToGame/convertToEntity.

---

#### P6. Factories de jeux codées en dur dans JpaGameDao

**Fichier** : `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/JpaGameDao.java:35-42`

**Code actuel** :
```java
factories.put("tictactoe", new TicTacToeGameFactory());
factories.put("connectfour", new ConnectFourGameFactory());
factories.put("taquin", new TaquinGameFactory());
```

**Problème** : Si on ajoute un nouveau jeu (nouveau `GamePlugin`), il faut aussi modifier `JpaGameDao`.
Cela viole le principe Open/Closed (ouvert à l'extension, fermé à la modification).

**Solution** : Injecter les `GamePlugin` dans `JpaGameDao` et construire la map des factories
depuis les plugins au lieu de les coder en dur :
```java
public JpaGameDao(GameEntityRepository repository, List<GamePlugin> plugins) {
    this.repository = repository;
    for (GamePlugin plugin : plugins) {
        factories.put(plugin.getGameType(), plugin.getFactory());
    }
}
```

**Complexité** : Faible.

---

#### P7. Pas de gestion d'erreurs globale

**Dossier vide** : `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/common/exception/`

**Problème** : Chaque erreur est gérée individuellement avec `ResponseStatusException` dans
`GameServiceImpl`. Si on veut ajouter du logging, du formatage, ou des codes d'erreur
cohérents, il faut modifier chaque throw.

**Solution** : Créer un `GlobalExceptionHandler` avec `@ControllerAdvice` :
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse(ex.getReason(), ex.getStatusCode().value()));
    }
}
```
+ un DTO `ErrorResponse` standardisé.

**Complexité** : Faible.

---

#### P8. `HeartbeatController` hors structure

**Fichier** : `/home/user/Documents/JavaSpring_Project/api_java_3_5/api/src/main/java/com/squaregames/api/HeartbeatController.java`

**Problème** : Ce contrôleur est à la racine du package `com.squaregames.api`,
pas dans un package feature ni dans `common/`. Il est mélangé avec `ApiApplication.java`.

**Solution** : Le déplacer dans `common/` ou un package `heartbeat/` dédié.

**Complexité** : Très faible — déplacement de fichier + ajustement d'imports.

---

### 🟢 Priorité basse — Améliorations futures

#### P9. Pas de migration de schéma (Flyway/Liquibase)

**Problème** : `spring.jpa.hibernate.ddl-auto=update` est utilisé pour la création/mise à jour
du schéma. C'est pratique en dev mais dangereux en production :
- Hibernate peut faire des modifications destructives (drop/recreate)
- Pas de traçabilité des changements de schéma
- Pas de rollback possible

**Solution** : Ajouter Flyway :
1. `spring.jpa.hibernate.ddl-auto=validate` (Hibernate vérifie mais ne modifie pas)
2. Créer des scripts de migration dans `src/main/resources/db/migration/`
3. Flyway les exécute dans l'ordre au démarrage

**Complexité** : Moyenne — dépend du nombre d'évolutions de schéma futures.

---

#### P10. Pas de documentation d'API (Swagger/OpenAPI)

**Problème** : Pas de documentation auto-générée des endpoints. Les seules docs sont le README
et les fichiers `explication*.md`.

**Solution** : Ajouter `springdoc-openapi-starter-webmvc-ui` :
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.x</version>
</dependency>
```
L'interface Swagger sera disponible à `http://localhost:8080/swagger-ui.html`.

**Complexité** : Faible.

---

#### P11. Pas de logging structuré

**Problème** : Pas de logs applicatifs (ni `slf4j`, ni `@Slf4j`). Le seul logging est celui
de Spring/Hibernate. Impossible de tracer les actions métier (création de partie, coup joué, etc.).

**Solution** : Ajouter `@Slf4j` sur les services et logguer les opérations clés :
```java
log.info("Partie créée : id={}, type={}, joueur={}", game.getId(), params.gameType(), userId);
log.info("Coup joué : partie={}, token={}, position=({},{})", gameId, move.tokenName(), move.row(), move.col());
```

**Complexité** : Très faible.

---

#### P12. Pas de DTO de versionnage / mapping

**Problème** : Les DTO (`GameDto`, `MoveRequest`, etc.) sont des records simples sans
versionnage. Si l'API évolue (champs renommés, ajoutés, supprimés), il n'y a pas de
mécanisme de compatibilité.

**Solution** : Pas urgent à ce stade, mais à considérer si l'API est consommée par
des clients externes. Utiliser des outils comme MapStruct pour le mapping
Game → GameDto si la complexité augmente.

---

## Tableau récapitulatif

| # | Problème | Priorité | Complexité | Impact |
|---|---------|----------|------------|--------|
| P1 | user-api en H2 fichier | ✅ Corrigé | Faible | Persistance des jeux partiellement inutile |
| P2 | findByPlayerId charge tout | 🔴 Haute | Faible | Performance dégradée avec le temps |
| P3 | Pas de @Transactional | 🔴 Haute | Faible | Risque d'incohérence de données |
| P4 | Entités JPA champs publics | 🟡 Moyenne | Moyenne | Pas d'encapsulation, difficile à refactorer |
| P5 | playerIds en String concaténée | 🟡 Moyenne | Moyenne | Recherche approximative, pas de FK |
| P6 | Factories codées en dur | 🟡 Moyenne | Faible | Violation Open/Closed |
| P7 | Pas de gestion d'erreurs globale | 🟡 Moyenne | Faible | Ne scale pas |
| P8 | HeartbeatController hors structure | 🟡 Moyenne | Très faible | Désordre dans le code |
| P9 | Pas de migration de schéma | 🟢 Basse | Moyenne | Risque en production |
| P10 | Pas de Swagger/OpenAPI | 🟢 Basse | Faible | Pas de doc auto-générée |
| P11 | Pas de logging structuré | 🟢 Basse | Très faible | Pas de traçabilité métier |
| P12 | Pas de versionnage DTO | 🟢 Basse | Moyenne | Pas de compat API |

---

## Ordre suggéré pour les améliorations

1. ~~**P1** — user-api en H2 fichier~~ ✅ Corrigé (29/05/2026)
2. **P3** — Ajouter `@Transactional` (5 min, sécurité des données)
3. **P2** — Requête JPQL pour findByPlayerId (15 min, performance)
4. **P6** — Factories depuis les plugins (15 min, évolutivité)
5. **P7** — GlobalExceptionHandler (30 min, qualité)
6. **P8** — Déplacer HeartbeatController (5 min, propreté)
7. **P4** — Encapsulation des entités (1h, dette technique)
8. **P5** — Table game_players (1h+, modèle de données)
9. **P11** — Logging structuré (30 min, observabilité)
10. **P9** — Flyway (2h, production-ready)
11. **P10** — Swagger (30 min, documentation)
12. **P12** — Versionnage DTO (selon besoin)

---

## Fichiers clés du projet

### App de jeux (port 8080)

| Fichier | Rôle |
|---------|------|
| `api_java_3_5/api/src/main/java/com/squaregames/api/ApiApplication.java` | Classe principale |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/api/GameController.java` | Contrôleur REST |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/api/GameCatalogController.java` | Catalogue de jeux |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/application/GameService.java` | Interface service |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/application/GameServiceImpl.java` | Implémentation service |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/application/GameDao.java` | Interface DAO |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/application/GamePlugin.java` | Interface plugin |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/application/UserValidator.java` | Interface validation |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/domain/GameEntity.java` | Entité JPA partie |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/domain/GameTokenEntity.java` | Entité JPA token |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/domain/GameEntityRepository.java` | Repository Spring Data |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/JpaGameDao.java` | DAO JPA (@Primary) |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/InMemoryGameDao.java` | DAO mémoire |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/JdbcGameDao.java` | DAO JDBC |
| `api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/RestUserValidator.java` | Validation via user-api |
| `api_java_3_5/api/src/main/resources/application.properties` | Config principale |
| `api_java_3_5/api/src/main/resources/application-h2.properties` | Config profil H2 |
| `api_java_3_5/api/src/main/resources/application-mysql.properties` | Config profil MySQL |
| `api_java_3_5/api/src/test/resources/application.properties` | Config test |

### user-api (port 8081)

| Fichier | Rôle |
|---------|------|
| `user-api/src/main/java/com/squaregames/user/UserApiApplication.java` | Classe principale |
| `user-api/src/main/java/com/squaregames/user/user/api/UserController.java` | Contrôleur REST |
| `user-api/src/main/java/com/squaregames/user/user/application/UserService.java` | Service |
| `user-api/src/main/java/com/squaregames/user/user/application/UserDao.java` | Interface DAO |
| `user-api/src/main/java/com/squaregames/user/user/domain/User.java` | Entité JPA |
| `user-api/src/main/java/com/squaregames/user/user/domain/UserRepository.java` | Repository Spring Data |
| `user-api/src/main/java/com/squaregames/user/user/infrastructure/JpaUserDao.java` | DAO JPA |

---

## Contraintes du moteur square-games-engine

Le moteur est une bibliothèque externe (JAR) dont le code source n'est pas modifiable.
Cela impose des contraintes importantes :

1. **Pas de sérialisation native** — Le moteur ne fournit pas de méthode `toJson()` ou `toMap()`.
   On doit extraire manuellement les données via `getBoard()`, `getRemainingTokens()`, etc.

2. **Reconstruction via `createGameWithIds`** — C'est la SEULE méthode pour recréer un jeu
   avec un état spécifique. Signature :
   ```java
   Game createGameWithIds(UUID gameId, int boardSize, List<UUID> playerIds,
                          Collection<TokenPosition<UUID>> onBoardTokens,
                          Collection<TokenPosition<UUID>> removedTokens)
   ```

3. **`TokenPosition` est un record** — `(E owner, String tokenName, int x, int y)`.
   C'est ce qu'on passe à `createGameWithIds` pour les tokens sur le plateau et les tokens retirés.

4. **Les tokens restants ne sont PAS passés** à `createGameWithIds` — le moteur les recalcule
   automatiquement à partir des tokens sur le plateau et du nombre total de tokens du jeu.

5. **`InconsistentGameDefinitionException`** — Si les données passées à `createGameWithIds`
   sont incohérentes (ex: token sur une position hors plateau), le moteur lève cette exception.
   Le fallback actuel est `factory.createGame(playerCount, boardSize)` (jeu vierge).

6. **`Token.getOwnerId()` retourne `Optional<UUID>`** — Pas `UUID`. Un bug classique
   est de faire `token.getOwnerId().toString()` qui produit `"Optional[uuid]"` au lieu de l'UUID.
   La correction : `token.getOwnerId().map(Object::toString).orElse(null)`.

7. **`Token.moveTo()` modifie l'objet en place** — Pas de retour. Le token est déplacé
   sur le plateau, et le `currentPlayerId` du jeu change automatiquement.

---

## Itération 5 — Spring Security + JWT (04/06/2026)

### Ce qui a été fait

| Changement | Fichiers |
|------------|----------|
| **user-api** : Spring Security stateless + JWT complet | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `AuthController`, `LoginRequest/Response` |
| **user-api** : `User` avec `password` et `role` | `User.java`, `UserServiceImpl`, `UserController` |
| **user-api** : `GlobalExceptionHandler` pour préserver les codes HTTP 400/409 malgré Spring Security | `GlobalExceptionHandler.java` |
| **api (jeux)** : `AuthenticationEntryPoint` pour retourner 401 au lieu de 403 quand JWT absent | `SecurityConfig.java` |
| **api (jeux)** : `GameController` utilise `SecurityContextHolder` au lieu de `X-UserId` | `GameController.java` |
| **api (jeux)** : Duplication de la sécurité JWT (`JwtService`, `JwtAuthenticationFilter`) | `common/security/*` |
| **Tests user-api** : Mis à jour pour JWT (14/14 passent) | `UserControllerIntegrationTest.java` |
| **Tests api** : Réécrits pour JWT, **tous passent** | `GameControllerIntegrationTest`, `ConnectFourAndTaquinIntegrationTest`, `JwtAuthContractTest` |
| **Documentation** | `explication5.md`, `suivi.md`, `README.md` |

### État des tests (05/06/2026)

- **user-api** : `BUILD SUCCESS` — 14/14 tests passent
- **api (jeux)** : `BUILD SUCCESS` — 60/60 tests passent

### Correctif post-implémentation — Ordre du filtre JWT

**Problème identifié** : le filtre JWT était enregistré avec `addFilterAfter(jwtAuthenticationFilter, SecurityContextHolderFilter.class)`. Cela le plaçait **trop tard** dans la chaîne de filtres — après la vérification d'authentification par Spring Security. Résultat : les requêtes avec un JWT valide étaient bloquées avec 401 avant d'atteindre le contrôleur.

De plus, l'endpoint `/error` (utilisé par Spring Boot pour retourner les erreurs métier) n'était pas autorisé, ce qui provoquait une réécriture des codes 404/400/403 en 401 lors du passage par `BasicErrorController`.

**Correction appliquée** dans `api_java_3_5/api/src/main/java/com/squaregames/api/common/security/SecurityConfig.java` :

```java
// Avant (incorrect) :
.addFilterAfter(jwtAuthenticationFilter, SecurityContextHolderFilter.class);

// Après (correct) :
.requestMatchers("/error").permitAll()   // ← permet aux erreurs métier de traverser
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

**Règle retenue** : le filtre JWT doit toujours être enregistré avec `addFilterBefore(UsernamePasswordAuthenticationFilter.class)` pour garantir que le `SecurityContext` est peuplé avant que Spring Security évalue les autorisations.

---

## Ce qui a déjà été corrigé

| Bug | Correction | Fichier |
|-----|-----------|---------|
| `Optional.toString()` au lieu de l'UUID | `getOwnerId().map(Object::toString).orElse(null)` | `JpaGameDao.convertToEntity` |
| `gameDao.upsert(game)` manquant après `token.moveTo()` | Ajout de l'appel | `GameServiceImpl.playMove` |
| H2 en mémoire (données perdues) | Passage en mode fichier | `application-h2.properties` |
| `InMemoryGameDao` était `@Primary` | `@Primary` déplacé sur `JpaGameDao` | Les deux DAOs |
| `PlaceholderResolutionException` dans les tests | Création de `src/test/resources/application.properties` | Tests |
| `squaregames.mv.db` pas dans `.gitignore` | Ajout de `data/` | `.gitignore` |
| user-api en H2 mémoire (P1) | Passage en mode fichier `jdbc:h2:file:./data/userdb` | `user-api/application.properties` |
| Plugins non testés | Ajout de `GamePluginTest` (12 tests unitaires) | `GamePluginTest.java` |
| Tests user-api cassés par H2 fichier | Création de `user-api/src/test/resources/application.properties` (H2 mémoire) | Tests user-api |

---

## Code obsolète / pédagogique

### JdbcGameDao — obsolète, conservé pour valeur pédagogique

**Fichier** : `api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/JdbcGameDao.java`

**Statut** : `@Repository` actif mais **jamais utilisé** car `JpaGameDao` est `@Primary`.

**Limitations majeures** :
- `mapRowToGame` crée un jeu **vierge** via `factory.createGame(playerCount, boardSize)` — l'état précédent (tokens, positions) est perdu
- `findByPlayerId` retourne `findAll()` sans filtrage — les playerIds ne sont pas stockés dans le schéma JDBC
- Pas de persistance des tokens (seules les métadonnées sont en base)

**Pourquoi le garder** :
- C'est le livrable de l'itération 3.3 (apprentissage JDBC avec SQL explicite)
- Montre la progression : InMemory → JDBC → JPA
- Sert de comparaison pédagogique pour comprendre les avantages de JPA

**Recommandation** : Ne pas supprimer, mais ajouter un commentaire `@Deprecated` pour indiquer qu'il ne doit plus être utilisé en production.

### InMemoryGameDao — utile pour le développement rapide

**Fichier** : `api_java_3_5/api/src/main/java/com/squaregames/api/game/infrastructure/InMemoryGameDao.java`

**Statut** : `@Repository` actif mais **pas @Primary**.

**Avantages** :
- Stockage par référence (l'objet Game est le même en mémoire) — pas de conversion nécessaire
- Rapide pour le développement et le debugging sans base de données
- `findByPlayerId` fonctionne correctement (filtrage sur les playerIds du Game)

**Inconvénients** :
- Données perdues au redémarrage
- Pas de persistance

**Recommandation** : Garder — utile comme DAO de fallback pour les tests de développement rapide.

---

## Analyse de couverture de tests

**Total** : 70 tests (60 api + 10 user-api), estimation ~65% de couverture.

| # | Item | Fait ? | Toujours utile ? | Priorité | Justification |
|---|------|--------|-----------------|----------|---------------|
| 1 | JdbcGameDao | ❌ | ❌ Obsolète | — | Code mort, `@Primary` sur JpaGameDao. Valeur uniquement pédagogique. |
| 2 | InMemoryGameDao | ❌ | ❌ Trop simple | — | HashMap basique, risque de bug quasi nul. Plus `@Primary`. |
| 3 | Plugins | ✅ | Fait | — | `GamePluginTest` : 12 tests unitaires (gameType, createGame, factory, noms). |
| 4 | GameCatalogImpl | ❌ | 🟡 Déjà couvert | Basse | `GameCatalogControllerTest` teste l'endpoint. Le service est simple. |
| 5 | Entités JPA | ❌ | ❌ Pas de logique | — | Champs publics, pas de méthodes. Testées indirectement via `JpaGameDaoTest`. |
| 6 | RestUserValidator | ❌ | ❌ Déjà couvert | — | `UserValidationContractTest` (WireMock) teste les 4 scénarios. |
| 7 | **ConnectFour / Taquin intégration** | ✅ | Fait | — | 8 tests d'intégration. Ont révélé 3 bugs (voir ci-dessous). |
| 8 | ConnectFour reconstruction | ✅ | Fait | — | `ConnectFourMoveTest` : 5 tests unitaires (reconstruction, normalisation, allowedMoves). |
| 9 | Persistance redémarrage | ❌ | 🟡 Complexe | Basse | Testé manuellement. Automatisation difficile (arrêt/redémarrage dans le test). |

### Bugs découverts par les tests ConnectFour / Taquin

**BUG-1 (CORRIGÉ) — Clés de factories incorrectes dans JpaGameDao et JdbcGameDao**
- Les factories étaient enregistrées avec les clés `"connectfour"` et `"taquin"`, mais le moteur retourne `"connect4"` et `"15 puzzle"` comme `factoryId`.
- Conséquence : `findById` et `findAll` retournaient `null` pour ces jeux → 404 sur `/moves` et `/games/{id}`.
- Correction : alignement des clés sur les `factoryId` du moteur (`"connect4"`, `"15 puzzle"`).

**BUG-2 (CORRIGÉ) — ConnectFour : reconstruction JPA incompatible avec createGameWithIds**
- Le moteur ConnectFour attend que les positions `y` soient `0, 1, 2…` consécutifs par colonne (indices à partir du bas).
- `JpaGameDao` stockait les positions réelles (gravité), ce qui provoquait `InconsistentGameDefinitionException` lors de la reconstruction.
- Par ailleurs, le moteur vérifie `counts[1] - counts[0] ∈ [0,1]`, ce qui exige que le joueur avec le plus de tokens soit à l'index 1 dans `playerIds`.
- **Corrections apportées** :
  - `ConnectFourStateAdapter` : normalise les positions y pour le moteur (regroupement par colonne, tri descendant, renumérotation).
  - `reorderPlayersForConnectFour` : réorganise `playerIds` pour respecter l'assertion du moteur.
  - `GameStateWrapper` : préserve l'`id` et le `currentPlayerId` originaux quand un fallback est nécessaire.
- Le marqueur de gravité `CellPosition(col, -1)` est bien interprété par le moteur. **ConnectFour est jouable via l'API REST**.

**BUG-3 (CORRIGÉ) — JpaGameDao : currentPlayerId et ID corrompus après rechargement**
- Après sauvegarde puis rechargement via `createGameWithIds`, le `currentPlayerId` et/ou l'`id` du jeu pouvaient changer (nouveaux UUID aléatoires).
- Cause : `createGameWithIds` peut lever `InconsistentGameDefinitionException` (jeux à gravité) ou produire un `currentPlayerId` incohérent.
- Le fallback `factory.createGame()` générait alors un jeu vierge avec un nouvel ID.
- **Corrections apportées** :
  - `GameStateWrapper` : wrapper délégué qui redéfinit `getId()` et `getCurrentPlayerId()` avec les valeurs stockées en base.
  - Stockage de `currentPlayerId` dans `GameEntity` pour le préserver après reconstruction.
  - Suppression du tri des `playerIds` dans `convertToEntity` (tri lexicographique qui cassait l'alternance TicTacToe).
- Impact : **TicTacToe, ConnectFour et Taquin sont utilisables après persistance JPA**.
