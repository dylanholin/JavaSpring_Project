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

### Structure du projet

**user-api/** (nouveau projet Spring Boot)
```
user-api/
├── pom.xml
├── src/main/java/com/squaregames/user/
│   ├── UserApiApplication.java
│   └── user/
│       ├── api/
│       │   ├── UserController.java
│       │   └── dto/
│       │       ├── UserCreationRequest.java
│       │       └── UserDto.java
│       ├── application/
│       │   ├── UserService.java
│       │   ├── UserServiceImpl.java
│       │   └── UserDao.java
│       ├── domain/
│       │   ├── User.java (entité JPA)
│       │   └── UserRepository.java
│       └── infrastructure/
│           └── JpaUserDao.java
└── src/main/resources/
    ├── application.properties (port 8081)
    └── schema.sql
```

### API REST exposée

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/users` | Créer un utilisateur |
| `GET` | `/users/{id}` | Récupérer un utilisateur |
| `DELETE` | `/users/{id}` | Supprimer un utilisateur |
| `GET` | `/users/{id}/valid` | Vérifier si l'utilisateur existe (booléen) |

### Configuration

**application.properties** (port 8081) :
```properties
server.port=8081
spring.application.name=user-api

# H2 Database (même config que l'app de jeux)
spring.datasource.url=jdbc:h2:mem:userdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
```

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
