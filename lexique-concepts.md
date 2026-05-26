# Lexique des concepts Spring Boot — Du théorie à la pratique

Ce document relie chaque concept pédagogique des itérations 1-4 à son implémentation concrète dans le projet. **Objectif** : savoir où trouver chaque concept dans le code.

---

## 1. IoC (Inversion of Control) & Injection de Dépendances

### Concept théorique
L'application ne crée pas elle-même ses objets. C'est Spring (le conteneur IoC) qui instancie et injecte les dépendances.

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

### Concept
Un bean = objet géré par le conteneur Spring. Les annotations marquent les classes pour que Spring les découvre.

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

### Concept
Séparation des responsabilités : API → Métier → Données.

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

### Concept
Objet qui transporte des données entre couches. **Ne jamais exposer les entités directement**.

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

### Concept
Programmer vers une interface = découplage. Spring injecte l'implémentation appropriée.

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

### Concept
Le moteur définit des comportements, chaque jeu les implémente différemment.

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

### Concept
Mapping objet-relationnel : une classe Java ↔ une table SQL.

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

### Concept
Externaliser les valeurs dans `application.properties` sans recompiler.

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

### Codes utilisés
- `201 CREATED` — Utilisateur créé
- `200 OK` — Succès
- `400 BAD_REQUEST` — Paramètres invalides, coup illégal
- `403 FORBIDDEN` — Utilisateur inconnu, pas son tour
- `404 NOT_FOUND` — Partie/utilisateur inexistant
- `409 CONFLICT` — Email déjà utilisé

---

## 12. Tests — Les 3 niveaux

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

**Ce qu'ils prouvent** : L'application fonctionne en vrai avec le moteur.

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
