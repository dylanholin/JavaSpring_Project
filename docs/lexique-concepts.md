# Lexique des concepts Spring Boot — Du théorie à la pratique

Ce document relie chaque concept pédagogique des itérations 1-4 à son implémentation concrète dans le projet. **Objectif** : savoir où trouver chaque concept dans le code.

---

## 1. IoC (Inversion of Control) & Injection de Dépendances

### Concept théorique

En programmation traditionnelle, quand une classe A a besoin d'une classe B, le développeur écrit directement `new B()` dans le code de A. Cela crée un couplage fort : A dépend directement de l'implémentation concrète de B. Si B change, il faut modifier A.

**L'Inversion of Control (IoC)** renverse cette logique. Ce n'est plus la classe A qui crée B, mais un conteneur externe — le conteneur Spring — qui instancie les objets et les injecte là où ils sont nécessaires. Le contrôle de la création est "inversé" : au lieu d'être dans le code applicatif, il est délégué au framework.

**L'injection de dépendances** est le mécanisme par lequel Spring fournit ces objets. Il existe trois façons d'injecter :
1. **Par constructeur** (recommandée) : Spring passe les dépendances en paramètres du constructeur
2. **Par setter** : Spring appelle des méthodes `setXxx()` après la création
3. **Par champ** (`@Autowired`) : Spring injecte directement dans les champs privés

**Pourquoi le constructeur est préféré ?**
- Les dépendances sont explicites : on les voit dans la signature du constructeur
- L'objet est immuable (`final`) : on ne peut pas le modifier après création
- Les tests sont plus simples : on peut mocker les dépendances en passant des faux objets
- Spring peut détecter les dépendances circulaires au démarrage

Dans notre projet, chaque classe déclare ses dépendances via `private final` et un constructeur. Spring, au démarrage, crée les objets dans le bon ordre et les injecte automatiquement.

### Dans le code — Injection par constructeur (préférée)
```java
// GameController.java — le contrôleur reçoit GameService sans faire 'new'
@RestController
@RequestMapping("/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {  // ← Spring injecte ici
        this.gameService = gameService;
    }
}
```

### Autres exemples dans le projet
- `GameServiceImpl` reçoit `GameDao`, `List<GamePlugin>`, `UserValidator`
- `RestUserValidator` reçoit `RestClient.Builder` et `@Value` pour l'URL
- Tous les plugins (`TicTacToePlugin`, `ConnectFourPlugin`) reçoivent `MessageSource`

**Pourquoi par constructeur ?** Les dépendances sont explicites, testables, immuables (`final`).

---

## 2. Beans & Annotations de composants

### Concept théorique

Un **bean** est simplement un objet Java dont le cycle de vie est géré par le conteneur Spring. Contrairement aux objets créés avec `new` dans une méthode classique, un bean est créé par Spring au démarrage de l'application, stocké dans un "conteneur", et injecté là où on en a besoin.

**Comment Spring découvre les beans ?**
Au démarrage, Spring scanne les packages à la recherche de classes annotées. Il les instancie, résout leurs dépendances, et les injecte dans les autres beans. C'est le **component scan**.

**Annotations de stéréotype** : Spring définit plusieurs annotations pour marquer les rôles :
- `@Component` : marqueur générique pour tout bean Spring
- `@Service` : marqueur pour la logique métier (même effet que `@Component`, mais plus sémantique)
- `@Repository` : marqueur pour la couche d'accès aux données (ajoute la gestion des exceptions JPA)
- `@RestController` : marqueur pour les contrôleurs REST (combine `@Controller` + `@ResponseBody`)
- `@SpringBootApplication` : marqueur du point d'entrée (combine `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`)

**Injection automatique de collections**
Quand Spring voit `List<GamePlugin>` dans un constructeur, il collecte automatiquement TOUS les beans qui implémentent `GamePlugin` et les injecte dans une liste. C'est le pattern "plugin" : ajouter un nouveau jeu = juste créer une nouvelle classe `@Component` qui implémente `GamePlugin`. Spring la découvre et l'ajoute à la liste sans modifier aucun code existant.

**Cycle de vie d'un bean**
1. Spring scanne les packages et trouve les classes annotées
2. Spring crée les instances (constructeur)
3. Spring injecte les dépendances
4. Spring appelle les méthodes d'initialisation (`@PostConstruct`)
5. Le bean est prêt à être utilisé
6. Au shutdown, Spring appelle les méthodes de destruction (`@PreDestroy`)

### Annotations utilisées

| Annotation | Rôle | Fichier(s) |
|------------|------|------------|
| `@SpringBootApplication` | Point d'entrée + auto-config + scan | `ApiApplication.java`, `UserApiApplication.java` |
| `@RestController` | Expose des endpoints HTTP | `GameController.java`, `UserController.java`, `GameCatalogController.java` |
| `@Service` | Logique métier | `GameServiceImpl.java`, `UserServiceImpl.java` |
| `@Component` | Composant générique | `TicTacToePlugin.java`, `ConnectFourPlugin.java`, `RestUserValidator.java` |
| `@Repository` (JPA) | Accès données | `GameEntityRepository.java`, `UserRepository.java` |

### Injection automatique de collections
```java
// GameServiceImpl.java — Spring injecte TOUS les @Component qui implémentent GamePlugin
private final List<GamePlugin> plugins;

public GameServiceImpl(..., List<GamePlugin> plugins, ...) {
    this.plugins = plugins;  // Contient TicTacToePlugin, ConnectFourPlugin, TaquinPlugin
}
```
**Avantage** : ajouter un nouveau jeu = juste créer une classe `@Component` qui implémente `GamePlugin`.

---

## 3. Architecture en Couches (Pattern Layered)

### Concept théorique

L'architecture en couches est un pattern fondamental qui sépare une application en plusieurs niveaux, chacun ayant une responsabilité unique. Cette séparation améliore la maintenance, les tests et la compréhension du code.

**Pourquoi séparer en couches ?**
- **Maintenabilité** : changer la base de données n'impacte pas l'API REST
- **Testabilité** : on peut tester chaque couche indépendamment
- **Lisibilité** : un développeur sait immédiatement où trouver quoi
- **Évolutivité** : ajouter un nouveau jeu ne touche que la couche application

**Les 4 couches du projet**

1. **Couche API** (`api/`) : Point d'entrée HTTP. Reçoit les requêtes, valide les paramètres, appelle la couche métier, et retourne les réponses JSON. Elle ne contient AUCUNE logique métier.

2. **Couche Application** (`application/`) : Contient la logique métier. Le service décide quoi faire (valider l'utilisateur, choisir un plugin, créer la partie). Il ne sait pas comment on parle HTTP ni comment on stocke en base.

3. **Couche Domaine** (`domain/`) : Contient les entités métier (objets JPA) et les repositories (interfaces de persistance). Elle définit CE QU'on stocke, pas COMMENT on le stocke.

4. **Couche Infrastructure** (`infrastructure/`) : Contient les implémentations techniques concrètes (JPA, JDBC, appels REST externes). C'est ici qu'on décide COMMENT on stocke et COMMENT on communique.

**Règle d'or** : une couche ne peut communiquer qu'avec la couche directement en dessous. Le contrôleur appelle le service, le service appelle le repository (via l'interface), jamais le contrôleur n'appelle directement le repository.

**Flux type d'une requête**
1. HTTP arrive sur `GameController.createGame()`
2. Le contrôleur appelle `gameService.createGame(params, userId)`
3. Le service valide l'utilisateur via `UserValidator`
4. Le service choisit le bon plugin et crée le jeu via le moteur
5. Le service appelle `gameDao.upsert(game)` pour persister
6. Le DAO convertit `Game` en `GameEntity` et appelle `repository.save()`
7. Le service convertit le résultat en `GameDto`
8. Le contrôleur retourne le DTO en JSON

### Dans le projet

```
┌─────────────────────────────────────────────────────────────────┐
│  COUCHE API (api/)                                              │
│  ───────────────────                                            │
│  • GameController.java      → @RestController, reçoit HTTP      │
│  • GameCatalogController.java → Catalogue des jeux disponibles  │
│  • dto/GameDto.java         → record, sortie JSON               │
│  • dto/GameCreationParams.java → record, entrée POST           │
└─────────────────────────────────────────────────────────────────┘
                              ↓ appelle
┌─────────────────────────────────────────────────────────────────┐
│  COUCHE APPLICATION (application/)                                │
│  ───────────────────────────────                                │
│  • GameService.java         → interface (contrat)               │
│  • GameServiceImpl.java     → @Service, logique métier         │
│  • GamePlugin.java          → interface pour les jeux         │
│  • TicTacToePlugin.java     → @Component, adapte le moteur    │
│  • UserValidator.java       → interface de validation user   │
│  • RestUserValidator.java   → @Component, appel REST user-api │
└─────────────────────────────────────────────────────────────────┘
                              ↓ appelle
┌─────────────────────────────────────────────────────────────────┐
│  COUCHE DOMAINE (domain/)                                       │
│  ────────────────                                               │
│  • GameEntity.java          → @Entity JPA, table SQL           │
│  • GameEntityRepository.java → @Repository, requêtes SQL         │
│  • User.java (user-api)     → @Entity, utilisateur             │
└─────────────────────────────────────────────────────────────────┘
                              ↓ appelle
┌─────────────────────────────────────────────────────────────────┐
│  COUCHE INFRASTRUCTURE (infrastructure/)                        │
│  ───────────────────────                                        │
│  • JpaGameDao.java          → implémentation concrète DAO        │
│  • JdbcGameDao.java         → alternative JDBC (obsolète)      │
│  • RestUserValidator.java   → adapter pour appeler user-api    │
└─────────────────────────────────────────────────────────────────┘
```

### Méthodes typiques du flux
1. **Controller** reçoit HTTP → appelle `gameService.createGame(params, userId)`
2. **Service** valide (UserValidator) → choisit un plugin → crée le jeu via le moteur
3. **DAO/Repository** sauvegarde l'entité JPA → retourne l'objet sauvegardé

---

## 4. DTO (Data Transfer Object)

### Concept théorique

Un **DTO (Data Transfer Object)** est un objet conçu uniquement pour transporter des données entre différentes parties d'une application. Contrairement aux entités métier qui contiennent de la logique et des relations complexes, un DTO est un simple conteneur de données.

**Pourquoi utiliser des DTO ?**

1. **Séparation des couches** : L'API REST ne doit jamais exposer directement les entités JPA (`GameEntity`) ni les objets du moteur externe (`Game`). Ces objets contiennent trop d'informations internes et créent un couplage fort entre l'API et la base de données.

2. **Contrôle du contrat** : Le DTO définit exactement quels champs sont exposés à l'API. Si le moteur change son `factoryId` en `gameType`, on adapte dans le DTO sans impacter les clients de l'API.

3. **Sécurité** : Les entités JPA peuvent contenir des champs sensibles (relations, mots de passe). Un DTO ne contient que ce qui est nécessaire.

4. **Immuabilité** : Les DTO du projet sont des `record` Java — immuables par défaut. Une fois créés, ils ne changent pas. Cela évite les effets de bord et facilite le raisonnement.

5. **Validation** : Les DTO d'entrée (comme `UserCreationRequest`) portent les annotations de validation (`@NotBlank`, `@Email`). C'est la première ligne de défense contre les données invalides.

**Types de DTO dans le projet**
- **DTO de sortie** (`GameDto`, `UserDto`) : ce que l'API retourne au client
- **DTO d'entrée** (`GameCreationParams`, `UserCreationRequest`) : ce que l'API reçoit du client
- **DTO interne** (`CatalogEntryDto`, `TokenMovesDto`) : utilisés entre les couches

**Règle** : chaque couche a son propre modèle de données. Le contrôleur reçoit un DTO d'entrée, le service travaille avec des objets métier, le DAO stocke des entités JPA. Les conversions se font via des méthodes `toDto()` et `toEntity()`.

### Dans le code

#### Record Java (immuables, concis)
```java
// api/game/api/dto/GameDto.java
public record GameDto(
    UUID id,
    String gameType,      // ← le moteur a "factoryId", on expose "gameType"
    int playerCount,
    int boardSize,
    String status,        // ← GameStatus.name() → String
    UUID currentPlayerId
) {}
```

#### Conversion Entity → DTO
```java
// UserController.java
private UserDto toDto(User user) {
    return new UserDto(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getCreatedAt()
    );
}
```

#### DTO d'entrée (Request)
```java
// api/game/api/dto/GameCreationParams.java
public record GameCreationParams(
    String gameType,
    int playerCount,
    int boardSize
) {}

// user-api/api/dto/UserCreationRequest.java — avec validation
public record UserCreationRequest(
    @NotBlank String name,
    @NotBlank @Email String email
) {}
```

**Règle** : Le moteur (`Game`) n'est jamais exposé à l'API. Il est converti en `GameDto`.

---

## 5. Interface & Implémentation

### Concept théorique

En programmation orientée objet, une **interface** définit un contrat — une liste de méthodes que toute classe implémentant cette interface doit fournir. L'interface ne contient pas d'implémentation, seulement la signature des méthodes.

**Pourquoi programmer vers une interface ?**

1. **Découplage** : Le code client (ex: `GameController`) ne dépend que de l'interface (`GameService`), pas de l'implémentation concrète (`GameServiceImpl`). On peut remplacer l'implémentation sans toucher le client.

2. **Testabilité** : Dans les tests unitaires, on peut créer un mock de l'interface (`MockGameService`) au lieu d'utiliser l'implémentation réelle qui pourrait avoir des effets de bord (base de données, appels réseau).

3. **Polymorphisme** : Plusieurs implémentations peuvent coexister. `GameDao` a eu trois implémentations successives : `InMemoryGameDao` (tests), `JdbcGameDao` (JDBC brut), `JpaGameDao` (Spring Data JPA). Le service `GameServiceImpl` n'a jamais eu besoin d'être modifié car il dépend de l'interface, pas de l'implémentation.

4. **Extension** : Ajouter un nouveau jeu = créer une nouvelle classe qui implémente `GamePlugin`. Le `GameServiceImpl` n'a pas besoin d'être modifié pour "connaître" ce nouveau jeu.

**Comment Spring gère les interfaces**
Quand Spring voit un constructeur qui demande une interface (ex: `GameService`), il cherche dans le conteneur un bean qui implémente cette interface. S'il n'en trouve qu'un, il l'injecte automatiquement. S'il en trouve plusieurs, il faut utiliser `@Primary` ou `@Qualifier` pour désigner celui à utiliser.

### Dans le projet

```java
// GameService.java — définit le CONTRAT
public interface GameService {
    GameDto createGame(GameCreationParams params, String userId);
    Collection<GameDto> listGames(String userId);
    GameDto getGame(UUID gameId);
    List<TokenMovesDto> getPossibleMoves(UUID gameId);
    GameDto playMove(UUID gameId, MoveRequest move, String userId);
}

// GameServiceImpl.java — implémentation concrète @Service
@Service
public class GameServiceImpl implements GameService {
    // ... implémentation avec validation user, plugin, DAO
}
```

### Pourquoi ?
- Le **contrôleur** ne connaît que `GameService` (interface)
- Le **test unitaire** peut mocker `GameService`
- On pourrait remplacer l'implémentation sans toucher le contrôleur

### Autres interfaces
- `GamePlugin` → implémenté par `TicTacToePlugin`, `ConnectFourPlugin`, `TaquinPlugin`
- `GameDao` → implémenté par `JpaGameDao` (avant `InMemoryGameDao`, `JdbcGameDao`)
- `UserValidator` → implémenté par `RestUserValidator`
- `UserDao` → implémenté par `JpaUserDao`

---

## 6. Méthodes abstraites & Polymorphisme

### Concept théorique

Le **polymorphisme** est l'un des quatre piliers de la programmation orientée objet (avec l'encapsulation, l'héritage et l'abstraction). Il signifie "plusieurs formes" : un même appel de méthode peut se comporter différemment selon l'objet réel qui l'exécute.

**Comment ça marche dans le projet ?**

Le moteur de jeu définit une interface `Game` avec des méthodes comme `getStatus()`, `getCurrentPlayerId()`, `moveTokenTo()`. Chaque jeu concret (TicTacToe, ConnectFour, Taquin) implémente cette interface, mais avec des règles différentes :
- TicTacToe : gagnant = 3 alignés sur plateau 3x3
- ConnectFour : gagnant = 4 alignés sur plateau 7x6
- Taquin : pas de gagnant, objectif = réordonner les tuiles

**Le pattern Plugin**

Notre projet utilise le pattern "Plugin" pour s'adapter au moteur :
1. Le moteur fournit une interface `GamePlugin` avec des méthodes `getFactory()`, `createGame()`, `getName()`
2. Chaque jeu a son propre plugin (`TicTacToePlugin`, `ConnectFourPlugin`, `TaquinPlugin`)
3. `GameServiceImpl` ne sait pas quel jeu concret il utilise — il appelle juste `plugin.createGame()`
4. Spring injecte automatiquement la liste de tous les plugins

**Avantage** : ajouter un nouveau jeu = créer une nouvelle classe qui implémente `GamePlugin`. Aucun code existant n'a besoin d'être modifié. C'est l'**Open/Closed Principle** : ouvert à l'extension, fermé à la modification.

**Méthodes abstraites**

Une méthode abstraite est une méthode déclarée dans une interface ou une classe abstraite, mais sans implémentation. C'est au code client de fournir l'implémentation. Dans notre projet, `GamePlugin.getGameType()` est une méthode abstraite — chaque plugin l'implémente différemment ("tictactoe", "connectfour", "taquin").

### Dans le moteur externe (bibliothèque)
```java
// fr.le_campus_numerique.square_games.engine.Game (interface moteur)
public interface Game {
    UUID getId();
    GameStatus getStatus();           // ONGOING / TERMINATED
    UUID getCurrentPlayerId();        // qui doit jouer (ou gagnant)
    Map<CellPosition, Token> getBoard();  // plateau
    Collection<Token> getRemainingTokens(); // tokens non joués
    void moveTokenTo(Token token, CellPosition pos); // jouer
}
```

### Dans nos plugins (adaptateurs)
```java
// TicTacToePlugin.java — adapte le moteur TicTacToe à notre API
@Component
public class TicTacToePlugin implements GamePlugin {
    private final TicTacToeGameFactory factory = new TicTacToeGameFactory();

    @Override
    public GameFactory getFactory() {
        return factory;  // ← le moteur sait créer des jeux TicTacToe
    }

    @Override
    public Game createGame() {
        return factory.createGame(defaultPlayerCount, defaultBoardSize);
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.tictactoe.name", null, locale);
    }

    @Override
    public String getGameType() {
        return factory.getGameFactoryId();  // "tictactoe"
    }
}
```

**Polymorphisme** : `GameServiceImpl` appelle `plugin.createGame()` sans savoir quel jeu c'est.

---

## 7. JPA (Java Persistence API) & Entités

### Concept théorique

**JPA (Java Persistence API)** est une spécification Java qui définit comment mapper des objets Java vers des tables de base de données relationnelle. C'est le **ORM (Object-Relational Mapping)**.

**Le problème**
Dans une application web, on manipule des objets Java (classe `User` avec `getName()`, `getEmail()`), mais les données sont stockées dans des tables SQL avec des lignes et des colonnes. JPA fait le pont entre ces deux mondes.

**Annotations JPA utilisées dans le projet**

- `@Entity` : marque une classe comme entité persistante (sera mappée à une table SQL)
- `@Table(name = "users")` : définit le nom de la table SQL
- `@Id` : marque le champ comme clé primaire
- `@Column(nullable = false, length = 100)` : configure la colonne SQL (NOT NULL, VARCHAR(100))
- `@OneToMany` / `@ManyToOne` : définit les relations entre entités (1-n, n-1)
- `@GeneratedValue` : génère automatiquement la valeur de la clé primaire

**Spring Data JPA**

Spring Data JPA est une couche au-dessus de JPA qui simplifie encore plus l'accès aux données. Au lieu d'écrire du SQL à la main, on définit une **interface** qui étend `JpaRepository<Entity, ID>`.

**Méthodes magiques** : Spring Data JPA génère AUTOMATIQUEMENT les requêtes SQL à partir du nom de la méthode :
- `findAll()` → `SELECT * FROM games`
- `findById(id)` → `SELECT * FROM games WHERE id = ?`
- `save(entity)` → `INSERT` ou `UPDATE`
- `deleteById(id)` → `DELETE`
- `findByNameContaining(String name)` → `SELECT * FROM users WHERE name LIKE %?%`

**Transactions**
Spring gère automatiquement les transactions JPA. Quand une méthode `@Service` est appelée, Spring ouvre une transaction. Si la méthode se termine normalement, la transaction est commitée. Si une exception est lancée, la transaction est rollbackée.

### Entité GameEntity
```java
// domain/GameEntity.java
@Entity
@Table(name = "games")
public class GameEntity {
    @Id
    private String id;              // UUID stocké en String

    private String factoryId;       // "tictactoe", "connect4"...
    private int boardSize;
    private int playerCount;
    private String status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "game_id")
    private List<GameTokenEntity> tokens = new ArrayList<>();
}
```

### Repository (DAO Spring Data JPA)
```java
// domain/GameEntityRepository.java
@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, String> {
    // Spring génère AUTOMATIQUEMENT la requête SQL
    List<GameEntity> findByPlayerIdsContaining(String playerId);
}
```

### Méthodes générées automatiquement
- `save(entity)` → INSERT ou UPDATE
- `findById(id)` → SELECT WHERE id = ?
- `deleteById(id)` → DELETE
- `findAll()` → SELECT *
- `findByPlayerIdsContaining(...)` → SELECT ... WHERE player_ids LIKE %?%

### Conversion Entité ↔ Objet métier
```java
// JpaGameDao.java
private GameEntity convertToEntity(Game game) {
    GameEntity entity = new GameEntity();
    entity.id = game.getId().toString();
    entity.factoryId = game.getFactoryId();
    // ... mapping des champs
    return entity;
}
```

---

## 8. @Value & Configuration externe

### Concept théorique

En développement, beaucoup de valeurs changent selon l'environnement : URL de services, nombres de joueurs par défaut, ports, clés API. Plutôt que de coder ces valeurs en dur dans le code Java, on les externalise dans des fichiers de configuration.

**Pourquoi externaliser ?**

1. **Sans recompiler** : changer `application.properties` suffit, pas besoin de modifier le code Java
2. **Environnements différents** : `application-dev.properties` pour le dev, `application-prod.properties` pour la production
3. **Sécurité** : les secrets (mots de passe, clés API) ne sont pas dans le code source
4. **Clarté** : toutes les valeurs configurables sont regroupées au même endroit

**Fichiers de configuration Spring Boot**

- `application.properties` (ou `application.yml`) : configuration par défaut
- `application-{profil}.properties` : configuration spécifique à un profil (dev, test, prod)
- Variables d'environnement : `SERVER_PORT=8080` remplace `server.port=8080`

**L'annotation @Value**

`@Value("${propriété}")` permet d'injecter une valeur de `application.properties` directement dans un champ ou un paramètre de constructeur. Spring lit le fichier au démarrage et remplace la propriété par sa valeur.

**Types de valeurs**
- Valeurs simples : `@Value("${user.service.url}")` → `"http://localhost:8081"`
- Valeurs numériques : `@Value("${game.tictactoe.default-player-count}")` → `2`
- Valeurs par défaut : `@Value("${timeout:5000}")` → `5000` si `timeout` n'est pas définie
- Expressions SpEL : `@Value("#{systemProperties['user.home']}")` → valeur système

### Dans application.properties
```properties
# Configuration des jeux (modifiable sans recompiler)
game.tictactoe.default-player-count=2
game.tictactoe.default-board-size=3
game.connectfour.default-player-count=2
game.connectfour.default-board-size=7

# URL du service utilisateurs
game.taquin.default-player-count=1
game.taquin.default-board-size=4

user.service.url=http://localhost:8081
```

### Injection avec @Value
```java
// TicTacToePlugin.java
@Component
public class TicTacToePlugin implements GamePlugin {
    @Value("${game.tictactoe.default-player-count}")
    private int defaultPlayerCount;

    @Value("${game.tictactoe.default-board-size}")
    private int defaultBoardSize;
}

// RestUserValidator.java — injection par constructeur
public RestUserValidator(RestClient.Builder restClientBuilder,
                         @Value("${user.service.url}") String userServiceUrl) {
    this.userServiceUrl = userServiceUrl;
}
```

---

## 9. REST API & Verbes HTTP

### Endpoints GameController
| Verbe | URL | Méthode Java | Description |
|-------|-----|--------------|-------------|
| `POST` | `/games` | `createGame()` | Créer une partie (header X-UserId requis) |
| `GET` | `/games` | `listGames()` | Lister mes parties |
| `GET` | `/games/{gameId}` | `getGame()` | Détails d'une partie |
| `GET` | `/games/{gameId}/moves` | `getPossibleMoves()` | Coups possibles |
| `POST` | `/games/{gameId}/moves` | `playMove()` | Jouer un coup |

### Endpoints UserController (user-api)
| Verbe | URL | Description |
|-------|-----|-------------|
| `POST` | `/users` | Créer utilisateur |
| `GET` | `/users/{id}` | Obtenir utilisateur |
| `GET` | `/users` | Lister tous |
| `DELETE` | `/users/{id}` | Supprimer |
| `GET` | `/users/{id}/valid` | Vérifier existence (booléen) |

### Annotations REST
```java
@RestController
@RequestMapping("/games")
public class GameController {

    @PostMapping  // ← POST /games
    public GameDto createGame(@RequestBody GameCreationParams params,
                              @RequestHeader("X-UserId") String userId) { ... }

    @GetMapping("/{gameId}")  // ← GET /games/{uuid}
    public GameDto getGame(@PathVariable UUID gameId) { ... }
}
```

---

## 10. Communication inter-services (Microservices)

### Concept théorique

Une architecture **microservices** découpe une application en plusieurs services indépendants, chacun ayant sa propre responsabilité. Contrairement à une architecture monolithique où tout est dans une seule application, les microservices communiquent entre eux via des API REST.

**Pourquoi microservices ?**

1. **Indépendance** : chaque service peut être développé, déployé et mis à l'échelle indépendamment
2. **Technologies différentes** : user-api pourrait utiliser Node.js, api pourrait utiliser Java
3. **Responsabilité unique** : user-api gère les utilisateurs, api gère les jeux
4. **Résilience** : si user-api tombe, api continue de fonctionner (avec des erreurs 403)

**Communication REST entre services**

Dans notre projet, `api` (port 8080) appelle `user-api` (port 8081) pour valider qu'un utilisateur existe avant de créer une partie. Cet appel est fait via **RestClient**, un client HTTP moderne de Spring Boot 3.2+.

**Le pattern Circuit Breaker implicite**

Quand `user-api` est inaccessible, `RestUserValidator` attrape l'exception (`RestClientException`) et retourne une erreur 403. C'est un comportement de type "fail-fast" : plutôt que d'attendre indéfiniment ou de réessayer, on refuse immédiatement la requête.

**Codes HTTP traduits**

| user-api répond | api traduit en | Pourquoi |
|-----------------|----------------|----------|
| 200 + `true` | continue (200) | L'utilisateur existe, on peut jouer |
| 200 + `false` | 403 FORBIDDEN | L'utilisateur n'existe pas |
| 404 Not Found | 403 FORBIDDEN | Le endpoint n'existe pas (service mal configuré) |
| Service down | 403 FORBIDDEN | Impossible de contacter user-api |

**Pourquoi toujours 403 ?** Pour le client de l'API, la raison exacte n'a pas d'importance. Que l'utilisateur n'existe pas ou que le service soit down, le résultat est le même : la requête est refusée. Le message d'erreur donne le détail pour le debug.

### Architecture
```
┌──────────────┐     GET /users/{id}/valid      ┌──────────────┐
│   api (8080) │ ───────────────────────────────→│ user-api     │
│              │                                 │   (8081)     │
│  RestUser    │ ←─────────────── 200 + true/false│              │
│  Validator   │                                 │              │
└──────────────┘                                 └──────────────┘
```

### Implémentation avec RestClient
```java
// RestUserValidator.java
@Component
public class RestUserValidator implements UserValidator {
    private final RestClient restClient;
    private final String userServiceUrl;

    @Override
    public void validate(String userId) {
        try {
            Boolean valid = restClient.get()
                .uri(userServiceUrl + "/users/{id}/valid", userId)
                .retrieve()
                .body(Boolean.class);

            if (!Boolean.TRUE.equals(valid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Utilisateur inconnu : " + userId);
            }
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Service utilisateurs inaccessible");
        }
    }
}
```

### Codes HTTP traduits
| user-api répond | api traduit en |
|-----------------|----------------|
| 200 + `true` | continue (créer la partie) |
| 200 + `false` | 403 FORBIDDEN |
| 404 Not Found | 403 FORBIDDEN |
| Service down | 403 FORBIDDEN |

---

## 11. Gestion des erreurs HTTP

### Concept théorique

En architecture REST, les codes HTTP ne sont pas juste des numéros — ils communiquent le résultat d'une opération au client. Une bonne API utilise les codes de manière cohérente et prévisible.

**Principe du "fail fast"**

Dès qu'une erreur est détectée, on arrête le traitement et on retourne immédiatement une erreur HTTP avec un message explicite. Cela évite de faire des calculs inutiles et donne au client une information claire.

**Flux d'erreur type dans le projet**

1. Le contrôleur reçoit une requête avec un `gameId` inexistant
2. Le service appelle `gameDao.findById(gameId)`
3. Le DAO retourne `Optional.empty()`
4. Le service lance `ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable : " + gameId)`
5. Spring convertit automatiquement cette exception en réponse HTTP 404 avec le message

**Hiérarchie des erreurs**

| Code | Quand l'utiliser | Dans le projet |
|------|-----------------|----------------|
| `200 OK` | Succès standard | Récupération d'une partie, liste des jeux |
| `201 CREATED` | Ressource créée avec succès | Création utilisateur |
| `400 BAD_REQUEST` | Paramètres invalides | Token inexistant, coup illégal, email invalide |
| `403 FORBIDDEN` | Accès refusé (pas authentifié/pas autorisé) | User inconnu, pas son tour, service down |
| `404 NOT_FOUND` | Ressource inexistante | Partie ou utilisateur non trouvé |
| `409 CONFLICT` | Conflit de ressource | Email déjà utilisé |

**Pourquoi ResponseStatusException ?**

`ResponseStatusException` est une exception de Spring qui associe directement un code HTTP à un message. Spring la convertit automatiquement en réponse HTTP. C'est plus simple que de créer une classe d'exception personnalisée pour chaque cas.

**Validation des entrées**

Les DTO d'entrée utilisent les annotations de validation Jakarta (`@NotBlank`, `@Email`). Spring les vérifie AUTOMATIQUEMENT avant d'appeler le contrôleur. Si la validation échoue, Spring retourne 400 BAD_REQUEST sans même entrer dans la méthode du contrôleur.

### Dans le service
```java
// GameServiceImpl.java
public GameDto getGame(UUID gameId) {
    Game game = gameDao.findById(gameId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Partie introuvable : " + gameId));
    return toDto(game);
}
```

---

## 12. Tests — Les 3 niveaux

### Concept théorique

Les tests sont essentiels pour garantir que le code fonctionne correctement et reste stable lors des modifications. Notre projet utilise une stratégie de tests à trois niveaux, chacun ayant un objectif différent.

**Pyramide des tests**

La pyramide des tests recommande d'avoir :
- Beaucoup de tests unitaires (rapides, isolés)
- Quelques tests d'intégration (lents mais réalistes)
- Très peu de tests end-to-end (très lents)

**Niveau 1 : Tests d'intégration (golden master)**

Ces tests démarrent une VRAIE application Spring Boot sur un port aléatoire (`RANDOM_PORT`). Ils appellent l'API via `TestRestTemplate` comme le ferait un vrai client. Seul le `UserValidator` est mocké pour éviter de dépendre de `user-api`.

**Pourquoi "golden master" ?** Ces tests servent de référence : si un refactoring casse un test d'intégration, c'est qu'on a changé le comportement observable de l'API. Ils ont découvert un vrai bug : le token était cherché dans `getBoard()` au lieu de `getRemainingTokens()`.

**Niveau 2 : Tests de contrat (WireMock)**

Ces tests vérifient la communication entre `api` et `user-api` sans démarrer `user-api`. WireMock simule le service externe en retournant des réponses HTTP prédéfinies.

**Pourquoi WireMock et pas Mockito ?** WireMock démarre un vrai serveur HTTP. Il teste que `RestUserValidator` fait réellement un appel réseau avec la bonne URL et interprète correctement chaque code HTTP. Un mock Mockito court-circuiterait tout le code HTTP.

**Niveau 3 : Tests unitaires (Mockito)**

Ces tests isolent une classe de toutes ses dépendances. On remplace `GameDao`, `UserValidator`, les plugins par des mocks. On vérifie que la classe sous test appelle les bonnes méthodes avec les bons paramètres.

**Limitation** : les mocks peuvent être "auto-cohérents" — si l'IA génère le service ET le test, elles peuvent partager la même hypothèse incorrecte. C'est pourquoi les tests d'intégration sont indispensables pour valider le comportement réel.

### Niveau 1 : Intégration (golden master)
**Fichiers** : `GameControllerIntegrationTest`, `UserControllerIntegrationTest`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerIntegrationTest {
    @Test
    void shouldCreateGame() {
        // Démarre vraiment l'app Spring Boot
        // Appelle l'API via TestRestTemplate
        // Vérifie la réponse HTTP et le JSON
    }
}
```

### Niveau 2 : Contrat (WireMock)
**Fichier** : `UserValidationContractTest`

```java
@EnableWireMock
class UserValidationContractTest {
    @InjectWireMock
    private WireMockServer wireMock;  // Simule user-api

    @Test
    void shouldReturn403WhenUserApiReturnsFalse() {
        wireMock.stubFor(get(...).willReturn(aResponse().withBody("false")));
        // Vérifie que api répond 403 quand user-api dit false
    }
}
```

### Niveau 3 : Unitaires (Mockito)
**Fichier** : `GameServiceImplTest`

```java
@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {
    @Mock private GameDao gameDao;
    @Mock private UserValidator userValidator;

    @Test
    void shouldCreateGame() {
        // Isolé : aucun vrai appel DAO ou moteur
        verify(userValidator).validate(USER_ID);
    }
}
```

---

## 13. Internationalisation (i18n)

### Concept théorique

L'**internationalisation (i18n)** permet à une application de s'adapter à différentes langues et cultures sans modification du code. C'est le "i" de i18n car il y a 18 lettres entre le "i" et le "n" de "internationalization".

**Pourquoi i18n ?**

1. **Utilisateurs internationaux** : l'API est utilisée par des développeurs francophones et anglophones
2. **Maintenance** : ajouter une langue = juste ajouter un fichier, pas modifier le code
3. **Séparation** : le texte n'est pas codé en dur dans le code Java

**MessageSource de Spring**

Spring fournit un bean `MessageSource` qui lit les fichiers `.properties` et retourne le bon message selon la `Locale` demandée. La Locale est extraite du header HTTP `Accept-Language`.

**Résolution des messages**

1. Le client envoie `Accept-Language: fr`
2. Spring crée une `Locale.FRENCH`
3. `messageSource.getMessage("game.tictactoe.name", null, locale)` cherche dans `messages_fr.properties`
4. Si le fichier ou la clé n'existe pas, Spring utilise `messages.properties` (fallback)

**Dans le projet**, seul le catalogue de jeux utilise l'i18n. Les messages d'erreur restent en français car ils sont destinés aux développeurs (pas aux utilisateurs finaux).

### Fichiers de messages
```properties
# messages.properties (défaut)
game.tictactoe.name=Tic-Tac-Toe
game.connectfour.name=Connect Four

# messages_fr.properties
/game.tictactoe.name=Morpion
```

### Utilisation
```java
// GameCatalogController.java
@GetMapping
public List<CatalogEntryDto> getCatalog(@RequestHeader(value = "Accept-Language", 
                                                       defaultValue = "en") Locale locale) {
    return plugins.stream()
        .map(p -> new CatalogEntryDto(
            p.getGameType(),
            p.getName(locale)  // ← MessageSource choisit le bon fichier
        ))
        .toList();
}
```

---

## 14. Maven & Dépendances

### Concept théorique

**Maven** est un outil de gestion de projet Java. Il gère :
- Les dépendances (bibliothèques externes)
- Le cycle de vie de build (compilation, tests, packaging)
- La structure standard du projet

**pom.xml**
Le fichier `pom.xml` (Project Object Model) est le cœur de Maven. Il définit :
- Les métadonnées du projet (groupId, artifactId, version)
- Les dépendances (avec leur version et scope)
- Les plugins de build
- Les repositories (où chercher les dépendances)

**Scopes de dépendances**
- `compile` (défaut) : disponible à la compilation et au runtime
- `test` : uniquement pour les tests (ex: WireMock, Mockito)
- `runtime` : pas besoin à la compilation, mais nécessaire à l'exécution (ex: driver H2)
- `provided` : fourni par l'environnement (ex: Tomcat embarqué par Spring Boot)

**Repositories**
Maven Central contient la majorité des bibliothèques open-source. Mais certaines dépendances (comme le moteur de jeu) sont hébergées sur des repositories privés (GitHub Packages). Il faut configurer l'URL du repository et l'authentification dans `~/.m2/settings.xml`.

**Parent POM**
Spring Boot fournit un "parent POM" (`spring-boot-starter-parent`) qui définit les versions compatibles de toutes les dépendances. On n'a pas besoin de spécifier la version de chaque starter Spring Boot.

### Dépendance privée (GitHub Packages)
```xml
<!-- pom.xml -->
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/le-campus-numerique/...</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>fr.le-campus-numerique.square-games</groupId>
        <artifactId>engine</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Dépendance test (WireMock)
```xml
<dependency>
    <groupId>org.wiremock.integrations</groupId>
    <artifactId>wiremock-spring-boot</artifactId>
    <version>3.9.0</version>
    <scope>test</scope>
</dependency>
```

---

## Table de correspondance rapide

| Concept | Cherche dans | Exemple concret |
|---------|--------------|-----------------|
| IoC / Injection | `*Controller.java`, `*ServiceImpl.java` | `GameController(GameService)` |
| Bean / Component | `@Component`, `@Service`, `@Repository` | `TicTacToePlugin` |
| Interface | `*.java` commençant par I ou dossier `application/` | `GameService`, `GamePlugin` |
| DTO | `dto/` | `GameDto`, `UserDto` |
| Entité JPA | `domain/`, `@Entity` | `GameEntity`, `User` |
| Repository | `domain/`, extends `JpaRepository` | `GameEntityRepository` |
| @Value | `application.properties` + plugins | `${game.tictactoe.default-board-size}` |
| REST | `@RestController`, `@GetMapping` | `GameController` |
| Inter-service | `@Value`, `RestClient` | `RestUserValidator` |
| Test intégration | `*IntegrationTest.java` | `GameControllerIntegrationTest` |
| Test contrat | `@EnableWireMock` | `UserValidationContractTest` |

---

## Comment utiliser ce lexique

1. **Tu cherches un concept** → trouve la section correspondante
2. **Tu veux voir l'implémentation** → suit les liens fichiers
3. **Tu dois expliquer en formation** → cite les exemples concrets du projet
4. **Tu modifies du code** → vérifie quel niveau d'architecture tu touches

**Règle d'or** : Chaque changement doit respecter la séparation des couches. Un contrôleur ne doit jamais appeler directement un Repository.
