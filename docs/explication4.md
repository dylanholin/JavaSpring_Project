# Itération 4 — Gestion des utilisateurs

## Objectif

Créer une **seconde application Spring Boot** dédiée à la gestion des utilisateurs, indépendante de l'application de jeux. Les deux services communiqueront via API REST.

**Architecture cible** :
```
┌─────────────────┐      REST API      ┌─────────────────┐
│  App JEUX       │ ◄────────────────► │  App USERS      │
│  (port 8080)    │   GET /users/{id}  │  (port 8081)    │
│                 │      /valid        │                 │
└─────────────────┘                    └─────────────────┘
```

⚠️ **Note de sécurité** : L'entête `X-UserId` utilisée dans cette itération n'est pas sécurisée (pas d'authentification). JWT sera implémenté dans l'itération 5.

---

## 4.1 — Création de l'API de gestion des utilisateurs

### Structure du projet (réalisée)

**user-api/** (nouveau projet Spring Boot indépendant)
```
user-api/
├── pom.xml                        ← Web + JPA + H2 + Validation
├── mvnw                           ← Wrapper Maven
└── src/main/java/com/squaregames/user/
    ├── UserApiApplication.java    ← Point d'entrée (@SpringBootApplication)
    └── user/
        ├── api/
        │   ├── UserController.java           ← REST : POST/GET/DELETE /users
        │   └── dto/
        │       ├── UserCreationRequest.java  ← record : name, email (validés)
        │       └── UserDto.java              ← record : id, name, email, createdAt
        ├── application/
        │   ├── UserService.java              ← Interface : contrat métier
        │   ├── UserServiceImpl.java          ← Logique métier (injecte UserDao)
        │   └── UserDao.java                  ← Interface DAO (abstraction)
        ├── domain/
        │   ├── User.java                     ← Entité JPA (@Entity, @Table)
        │   └── UserRepository.java           ← Spring Data JpaRepository
        └── infrastructure/
            └── JpaUserDao.java               ← Implémentation DAO via JPA
```

### Points clés de l'architecture

**Pourquoi séparer `UserDao` du `UserRepository` ?**

C'est le même principe que dans l'app de jeux avec `GameDao` :
- `UserRepository` : interface Spring Data (technologie JPA)
- `UserDao` : interface métier (indépendante de la technologie)
- `JpaUserDao` : la "colle" entre les deux

```
UserController → UserService → UserDao ← (interface)
                                             ↑
                                         JpaUserDao → UserRepository → Base H2
```

Si demain tu changes de BDD ou technologie, tu crées un `MongoUserDao` ou `JdbcUserDao` sans toucher au service ni au controller.

### Entité User en détail

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 36)
    private String id;         // UUID généré dans le constructeur

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;      // Contrainte UNIQUE en base

    @Column(name = "created_at")
    private Instant createdAt; // Généré automatiquement dans le constructeur

    public User() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
}
```

💡 **Pourquoi `String id` et pas `@GeneratedValue` ?**
On génère l'UUID en Java pour avoir l'id **avant** la persistance. Cela permet de le retourner immédiatement dans la réponse HTTP (201 Created) sans faire de SELECT supplémentaire.

### DTOs : les records Java

```java
// Entrée — validation avec @NotBlank et @Email
public record UserCreationRequest(
    @NotBlank String name,
    @NotBlank @Email String email
) {}

// Sortie — lecture seule, jamais l'entité JPA directement
public record UserDto(String id, String name, String email, Instant createdAt) {}
```

💡 **Règle** : on n'expose jamais directement les entités JPA dans l'API. Les DTOs sont des "vues" de lecture conçues pour le client HTTP.

### Validation automatique avec `@Valid`

```java
@PostMapping
public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreationRequest request) {
    // Spring valide automatiquement les contraintes avant d'appeler la méthode
    // Si invalide → 400 Bad Request automatique
}
```

La dépendance `spring-boot-starter-validation` active `@NotBlank`, `@Email`, `@Size`, etc.

### API REST exposée

| Méthode | Endpoint | Code retour | Description |
|---------|----------|-------------|-------------|
| `POST` | `/users` | `201 Created` | Créer un utilisateur |
| `GET` | `/users/{id}` | `200` / `404` | Récupérer par id |
| `GET` | `/users` | `200` | Lister tous les utilisateurs |
| `DELETE` | `/users/{id}` | `204 No Content` | Supprimer |
| `GET` | `/users/{id}/valid` | `200` + `true/false` | Vérifier existence (pour l'app de jeux) |

### Configuration

**application.properties** (port 8081) :
```properties
server.port=8081
spring.application.name=user-api

spring.datasource.url=jdbc:h2:mem:userdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

💡 **`ddl-auto=update`** : Hibernate crée/modifie les tables automatiquement au démarrage. En production on utiliserait `validate` (vérifie que le schéma correspond) ou `none`.

---

## 4.2 — Modification de l'API de jeux de plateau

### Entête X-UserId (réalisé)

Les endpoints `POST /games`, `GET /games` et `POST /games/{id}/moves` reçoivent maintenant l'identifiant du joueur :

```java
@PostMapping
public GameDto createGame(@RequestBody GameCreationParams params,
                          @RequestHeader("X-UserId") String userId) {
    return gameService.createGame(params, userId);
}

@GetMapping
public Collection<GameDto> listGames(@RequestHeader("X-UserId") String userId) {
    return gameService.listGames(userId);
}

@PostMapping("/{gameId}/moves")
public GameDto playMove(@PathVariable UUID gameId, @RequestBody MoveRequest move,
                        @RequestHeader("X-UserId") String userId) {
    return gameService.playMove(gameId, move, userId);
}
```

💡 **`@RequestHeader("X-UserId")`** : Spring injecte automatiquement la valeur de l'entête HTTP. Si l'entête est absent, Spring retourne `400 Bad Request`.

### Architecture — interface UserValidator

Pour garder le service testable, la validation utilisateur est extraite dans une interface dédiée :

```
GameController
     │ X-UserId header
     ▼
GameServiceImpl
     │ userValidator.validate(userId)
     ▼
UserValidator ←── interface (testable par mock)
     ▲
RestUserValidator ←── implémentation (appelle user-api via RestClient)
```

**Pourquoi une interface ?**
Sans interface, on ne peut pas mocker facilement l'appel HTTP dans les tests d'intégration.
Avec l'interface, on injecte un mock dans les tests → zéro dépendance vers user-api pendant les tests.

### Interface UserValidator

```java
public interface UserValidator {
    void validate(String userId);
    // Lance ResponseStatusException 403 si l'utilisateur est inconnu
}
```

### Implémentation RestUserValidator

```java
@Component
public class RestUserValidator implements UserValidator {

    private final RestClient restClient;
    private final String userServiceUrl;

    public RestUserValidator(RestClient.Builder builder,
                             @Value("${user.service.url}") String userServiceUrl) {
        this.restClient = builder.build();
        this.userServiceUrl = userServiceUrl;
    }

    @Override
    public void validate(String userId) {
        try {
            Boolean valid = restClient.get()
                .uri(userServiceUrl + "/users/{id}/valid", userId)
                .retrieve()
                .body(Boolean.class);
            if (!Boolean.TRUE.equals(valid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur inconnu");
            }
        } catch (RestClientException e) {
            // user-api inaccessible → 403
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service inaccessible");
        }
    }
}
```

💡 **`RestClient.Builder`** : Spring Boot 3.x fournit un `RestClient.Builder` préconfiguré en tant que bean. On l'injecte par constructeur et on appelle `.build()`.

💡 **`@Value("${user.service.url}")`** : lit la propriété dans `application.properties`. Cela permet de changer l'URL sans recompiler.

### Règles métier avec identification

| Fonctionnalité | Règle métier | Code HTTP |
|----------------|--------------|-----------|
| **Créer partie** | Valide `X-UserId` via user-api | `403` si inconnu |
| **Lister parties** | Filtre par `findByPlayerId(userId)` dans le DAO | Liste filtrée |
| **Jouer coup** | Vérifie `currentPlayerId == userId`, sinon refuse | `403 Forbidden` |
| **Header absent** | Spring rejette automatiquement | `400 Bad Request` |

### GameDao — nouvelle méthode findByPlayerId

```java
// Interface GameDao (application)
Collection<Game> findByPlayerId(String playerId);

// InMemoryGameDao (infrastructure)
@Override
public Collection<Game> findByPlayerId(String playerId) {
    return games.values().stream()
        .filter(game -> game.getPlayerIds().stream()
            .map(Object::toString)
            .anyMatch(playerId::equals))
        .toList();
}
```

💡 **Pourquoi `Object::toString` ?** Le moteur stocke les `playerIds` en `Collection<UUID>` alors que `X-UserId` est une `String`. On convertit les UUID en String pour comparer correctement.

### GameDto — ajout de currentPlayerId

```java
public record GameDto(
    UUID id,
    String gameType,
    int playerCount,
    int boardSize,
    String status,
    UUID currentPlayerId  // ← ajouté pour que le client sache à qui c'est le tour
) {}
```

### Comprendre les différents IDs dans le système

Le système utilise **trois types d'IDs différents** qui ont des rôles distincts :

| Type d'ID | Source | Rôle | Stabilité |
|-----------|--------|------|-----------|
| **ID utilisateur** | `user-api` (création utilisateur) | Identifie l'utilisateur dans toute l'application | Ne change jamais |
| **ID de partie** | `api` (création partie) | Identifie la partie spécifique pour jouer/lister | Ne change jamais |
| **currentPlayerId** | `api` (réponse création/partie) | Indique à qui est le tour de jouer | **Change après chaque coup** |

**Exemple de flux :**

1. Créer Alice → `82e6a9eb-...` (ID utilisateur)
2. Créer une partie → `34950b83-...` (ID partie), `currentPlayerId = 82e6a9eb-...` (c'est à Alice)
3. Alice joue → `currentPlayerId = fe92625a-...` (c'est à l'adversaire)
4. Adversaire joue → `currentPlayerId = 82e6a9eb-...` (c'est à nouveau à Alice)

**Pourquoi `currentPlayerId` change ?** C'est la logique du jeu de plateau — chaque joueur joue à son tour. Le champ indique "à qui est-ce le tour maintenant ?" et permet au serveur de vérifier que le joueur qui tente de jouer est bien celui dont c'est le tour (sinon `403 Forbidden`).

### Configuration (application.properties)

```properties
# URL du service utilisateurs (user-api)
user.service.url=http://localhost:8081
```

### Stratégie de test avec @MockitoBean

Dans les tests d'intégration, on ne veut pas démarrer user-api. On mock `UserValidator` :

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerIntegrationTest {

    @MockitoBean
    private UserValidator userValidator; // ← remplace le bean Spring par un mock

    @BeforeEach
    void setUp() {
        doNothing().when(userValidator).validate(anyString()); // accepte tout userId
    }
}
```

💡 **`@MockitoBean`** (Spring Boot 3.4+, remplace `@MockBean` déprécié) : remplace le vrai bean Spring par un mock Mockito dans le contexte d'application de test.

---

## Livrables attendus

- ✅ Application **user-api** (port 8081) avec CRUD utilisateurs
- ✅ Application **api** (port 8080) mise à jour avec `X-UserId`
- ✅ Communication REST entre les deux services
- ✅ Documentation dans `README.md` de chaque projet

## Ressources

- [Baeldung — RestClient Spring Boot](https://www.baeldung.com/spring-boot-restclient)
- [GeeksforGeeks — Guide RestClient](https://www.geeksforgeeks.org/a-guide-to-restclient-in-spring-boot/)

---

## ⚠️ Attention — Les exemples curl de l'itération 3 sont obsolètes

Depuis l'itération 4.2, le header `X-UserId` est **obligatoire** sur les endpoints `/games` (POST, GET) et `/games/{id}/moves` (POST). Les exemples curl de `explication3.md` ne l'incluent pas et retourneront désormais `400 Bad Request`.

**Exemple mis à jour pour POST /games** :
```bash
# Avant (itération 3) — ne fonctionne plus
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'

# Après (itération 4.2) — version correcte
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -H "X-UserId: <uuid-joueur-valide>" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'
```

> 📌 `<uuid-joueur-valide>` doit être l'id d'un utilisateur existant dans user-api (créé via `POST /users`).

---

## Notes d'implémentation

**Ports distincts obligatoires** :
- App de jeux : `8080`
- App utilisateurs : `8081`

**Démarrage des deux applications** :
```bash
# Terminal 1 : App de jeux
cd api_java_3_5/api
./mvnw spring-boot:run

# Terminal 2 : App utilisateurs
cd user-api
./mvnw spring-boot:run
```
