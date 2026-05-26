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

### Entête X-UserId

Toutes les requêtes de jeu doivent inclure l'entête `X-UserId` :
```java
@GetMapping("/games")
public List<GameDto> getMyGames(
    @RequestHeader("X-UserId") String userId
) {
    // Ne retourner que les parties du joueur
}
```

### Règles métier avec identification

| Fonctionnalité | Règle |
|----------------|-------|
| **Créer partie** | Associer le `X-UserId` comme créateur/joueur |
| **Lister parties** | Ne retourner que celles où `X-UserId` participe |
| **Jouer coup** | Vérifier que `currentPlayerId == X-UserId`, sinon `403 Forbidden` |
| **Validation** | Appeler `GET /users/{id}/valid` sur le service utilisateurs |

### Communication inter-services avec RestClient

**Configuration** :
```java
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient userServiceRestClient(@Value("${user.service.url}") String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
```

**application.properties** (app de jeux) :
```properties
user.service.url=http://localhost:8081
```

**Utilisation dans le service** :
```java
@Service
public class GameServiceImpl implements GameService {
    
    private final RestClient userServiceClient;
    
    public GameServiceImpl(RestClient userServiceClient, ...) {
        this.userServiceClient = userServiceClient;
    }
    
    private boolean isValidUser(String userId) {
        Boolean exists = userServiceClient.get()
            .uri("/users/{id}/valid", userId)
            .retrieve()
            .body(Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}
```

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
