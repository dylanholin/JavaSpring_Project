# Comment jouer une partie

Guide pas-à-pas basé sur l'API existante (itération 2).

> 📁 Racine Java : `api_java_3_5/api/src/main/java/com/squaregames/api/game/`

---

## 0. Démarrer l'application

Avant de tester les endpoints, il faut lancer le serveur Spring Boot.

```bash
cd /home/user/Documents/JavaSpring_Project/api_java_3_5/api
./mvnw spring-boot:run
```

Ou depuis ton IDE : clique droit sur `ApiApplication.java` → Run.

Le serveur démarre sur `http://localhost:8080` par défaut. Tu verras un log comme :

```
Started ApiApplication in X.xxx seconds
```

Une fois démarré, laisse le terminal ouvert — le serveur tourne en arrière-plan.

---

## 1. Créer une partie

```http
POST http://localhost:8080/games
Content-Type: application/json

{
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3
}
```

**Réponse :**
```json
{
  "id": "a1b2c3d4-...",
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3,
  "status": "ONGOING"
}
```

> 📌 Garde l'`id`, il sert pour toutes les actions suivantes.

## 2. Voir les coups possibles

```http
GET http://localhost:8080/games/{id}/moves
```

> ⚠️ **Important** : remplace `{id}` par l'UUID réel retourné par l'étape 1.
>
> Exemple : si l'UUID est `a2ac5ed2-7761-41d2-a794-eb8e3b919252`, l'URL devient :
> `http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves`

## 3. Jouer un coup

```http
POST http://localhost:8080/games/{id}/moves
Content-Type: application/json

{
  "tokenName": "X",
  "row": 1,
  "col": 1
}
```

> ⚠️ Remplace `{id}` par l'UUID réel de ta partie.

## 4. Voir l'état de la partie

```http
GET http://localhost:8080/games/{id}
```

> ⚠️ Remplace `{id}` par l'UUID réel de ta partie.

## 5. Lister toutes les parties

```http
GET http://localhost:8080/games
```

## 6. Catalogue des jeux

```http
GET http://localhost:8080/games/catalog
Accept-Language: fr
```

---

## Exemple complet avec curl

Voici une séquence complète de test avec `curl` :

```bash
# 1. Créer une partie
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'

# Réponse : {"id":"a2ac5ed2-7761-41d2-a794-eb8e3b919252",...}

# 2. Voir les coups possibles (remplace l'UUID par celui reçu)
curl http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves

# 3. Jouer un coup X en (0,0)
curl -X POST http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves \
  -H "Content-Type: application/json" \
  -d '{"tokenName":"X","row":0,"col":0}'

# 4. Jouer un coup 0 en (1,1)
curl -X POST http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves \
  -H "Content-Type: application/json" \
  -d '{"tokenName":"0","row":1,"col":1}'
```

---

## Résumé du flux

```
POST   /games              → crée une partie
GET    /games/{id}/moves   → coups possibles
POST   /games/{id}/moves   → joue un coup
GET    /games/{id}         → état de la partie
GET    /games              → toutes les parties
GET    /games/catalog      → jeux disponibles
```

---

# Itération 3 — Persistance des données

## 🎯 Objectif

L'application actuelle stocke les parties en mémoire (`Map<UUID, Game>`). Toutes les données sont perdues à chaque redémarrage. Cette itération introduit la persistance via une base de données relationnelle, en progressant graduellement : DAO en mémoire → JDBC → JPA.

---

## 3.1 — Le pattern DAO

**Concept** : Le DAO (Data Access Object) sépare la logique métier de la logique d'accès aux données. Le DAO expose des opérations de base (CRUD) et cache les détails de la technologie de persistance.

**Avantage clé** : changer de technologie (mémoire → JDBC → JPA) n'impacte pas la couche service.

```java
public interface GameDao {
    Stream<Game> findAll();
    Optional<Game> findById(String gameId);
    Game upsert(Game game);  // insert ou update
    void delete(String gameId);
}
```

---

## 3.2 — Mise en place du DAO en mémoire

**Objectif** : refactoriser l'existant pour isoler la persistance avant d'ajouter une vraie base de données.

**Étapes** :
1. Créer l'interface `GameDao` dans `game/application`
2. Modifier `GameServiceImpl` pour injecter `GameDao` au lieu d'utiliser directement une `Map`
3. Créer `InMemoryGameDao` dans `game/infrastructure` qui utilise `Map<String, Game>`
4. Faire passer toutes les opérations de persistance par `GameDao`

> ⚠️ Cette implémentation se réinitialise à chaque démarrage — c'est normal à ce stade.

### Implémentation réalisée

**Interface `GameDao`** (`game/application/GameDao.java`) :
```java
public interface GameDao {
    Collection<Game> findAll();
    Optional<Game> findById(UUID gameId);
    Game upsert(Game game);  // sauvegarde ou met à jour
    void delete(UUID gameId);
}
```

**Implémentation `InMemoryGameDao`** (`game/infrastructure/InMemoryGameDao.java`) :
```java
@Repository
public class InMemoryGameDao implements GameDao {
    private final Map<UUID, Game> games = new HashMap<>();

    @Override
    public Collection<Game> findAll() {
        return games.values();
    }

    @Override
    public Optional<Game> findById(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public Game upsert(Game game) {
        games.put(game.getId(), game);
        return game;
    }

    @Override
    public void delete(UUID gameId) {
        games.remove(gameId);
    }
}
```

**Refactoring de `GameServiceImpl`** :
- Avant : `private final Map<UUID, Game> games = new HashMap<>();`
- Après : `private final GameDao gameDao;` (injection par constructeur)

Les méthodes utilisent maintenant :
- `gameDao.upsert(game)` au lieu de `games.put(...)`
- `gameDao.findAll()` au lieu de `games.values()`
- `gameDao.findById(id).orElseThrow(...)` au lieu de `games.get(id)` avec null-check

### Avantages du pattern DAO

1. **Séparation des responsabilités** : la couche service ne sait pas comment les données sont stockées
2. **Testabilité** : on peut mock `GameDao` dans les tests unitaires
3. **Évolutivité** : passer de `InMemoryGameDao` à `JdbcGameDao` ou `JpaGameDao` sans toucher au service

---

## Tests — Golden Master (bonus)

**Objectif** : créer une suite de tests qui servira de référence pour valider que les refactorings futurs (JDBC, JPA) ne cassent pas le comportement existant.

### Types de tests créés

**1. Tests d'intégration API** (`GameControllerIntegrationTest.java`) :
- `@SpringBootTest` avec `TestRestTemplate` pour tester les endpoints réellement
- Tests de création, récupération, liste, jeu de coups
- Vérification des codes HTTP (200, 404, 400)

**2. Tests catalogue** (`GameCatalogControllerTest.java`) :
- Test du endpoint `/games/catalog`
- Vérification du support i18n avec header `Accept-Language`

**3. Tests unitaires Service** (`GameServiceImplTest.java`) :
- `@ExtendWith(MockitoExtension.class)` pour isoler le service
- Mock de `GameDao` et `GamePlugin` pour tester sans dépendances
- Vérification des interactions avec les mocks

### Pourquoi c'est utile

- **Golden Master** : après le refactoring DAO, les tests doivent toujours passer
- **Régression** : détecter immédiatement si une modification casse l'API
- **Documentation** : les tests montrent comment l'API est censée fonctionner

### Exécuter les tests

```bash
cd /home/user/Documents/JavaSpring_Project/api_java_3_5/api
./mvnw test
```

---

## 3.3 — Implémentation du DAO avec JDBC

**Objectif** : stocker les données dans une vraie base SQL via JDBC (SQL explicite).

**Étapes** :
1. Lancer une base PostgreSQL ou MySQL via Docker
2. Ajouter la dépendance `spring-boot-starter-jdbc`
3. Configurer `application.properties` avec les propriétés de connexion
4. Ajouter le driver JDBC (`postgresql` ou `mysql-connector-java`)
5. Créer `JdbcGameDao` dans `game/infrastructure`
6. Injecter `NamedParameterJdbcTemplate` et exécuter des requêtes SQL

**Ressources** :
- [Baeldung — Spring JDBC](https://www.baeldung.com/spring-jdbc-jdbctemplate)
- [DZone — NamedParameterJdbcTemplate](https://dzone.com/articles/spring-namedparameterjdbctemplate)

---

## 3.4 — Implémentation du DAO avec JPA

**Objectif** : utiliser Spring Data JPA pour ne plus écrire de SQL manuellement.

**Étapes** :
1. Ajouter la dépendance `spring-boot-starter-data-jpa`
2. Créer les entités JPA dans `game/domain` :
   - `GameEntity` avec `@Id`, `@Entity`
   - `GameTokenEntity` pour les tokens avec `@OneToMany`
3. Créer `GameEntityRepository` qui étend `JpaRepository<GameEntity, String>`
4. Créer `JpaGameDao` qui implémente `GameDao` en utilisant le repository
5. Implémenter les conversions `Game ↔ GameEntity`

**Annotations JPA clés** :
- `@Entity` : marque une classe comme entité persistante
- `@Id` : identifiant primaire
- `@OneToMany(cascade = CascadeType.ALL)` : relation avec suppression en cascade

**Ressources** :
- [Baeldung — Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-and-jpa)
- [Spring — Accessing Data](https://spring.io/guides/gs/accessing-data-mysql/)

---

## 3.5 — Bonus : Profils Spring et sources de données multiples

**Objectif** : basculer facilement entre H2 (tests) et une vraie base (production).

**Étapes** :
1. Créer `application-h2.properties` pour la config H2 en mémoire
2. Créer `application-mysql.properties` (ou `application-postgres.properties`) pour la prod
3. Activer un profil via `spring.profiles.active=h2` ou `--spring.profiles.active=mysql`

**Avantage** : exécuter les tests rapidement sur H2, déployer sur PostgreSQL.

---

## Livrables attendus

- Interface `GameDao` et implémentation `InMemoryGameDao`
- Implémentation `JdbcGameDao` avec SQL explicite
- Implémentation `JpaGameDao` avec Spring Data JPA
- Entités JPA `GameEntity` et `GameTokenEntity`
- Les parties survivent au redémarrage de l'application
