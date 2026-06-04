# Itération 5 — Sécurisation avec Spring Security et JWT

> **Commit de référence avant cette itération :** `c02ae3b` (`docs: explique l'architecture hexagonale (ports/adapters) dans le README`)
>
> Tous les changements décrits ci-dessous ont été effectués après ce commit.

---

## Objectif

Remplacer l'authentification naïve par header `X-UserId` (transmission directe de l'identifiant utilisateur) par un mécanisme robuste fondé sur **JSON Web Tokens (JWT)** et **Spring Security**.

**Architecture cible :**
```
┌─────────────────┐      REST API        ┌─────────────────┐
│  App JEUX       │  Authorization:      │  App USERS      │
│  (port 8080)    │  Bearer <JWT>        │  (port 8081)    │
│                 │ ◄──────────────────► │                 │
│  Validation     │                      │  Émission       │
│  locale JWT     │                      │  JWT (login)    │
└─────────────────┘                      └─────────────────┘
```

⚠️ **Note de sécurité** : L'entête `X-UserId` de l'itération 4 est supprimé. L'authentification se fait désormais via un token JWT signé, vérifiable localement par l'API de jeux sans appel réseau.

---

## Contexte et problématique

L'itération 4 utilisait un header `X-UserId` pour identifier l'utilisateur. Ce mécanisme présente plusieurs failles de sécurité :

- **Aucune vérification d'identité** : n'importe quel client peut envoyer n'importe quel `userId`
- **Pas d'authentification** : impossible de savoir si l'utilisateur est réellement celui qu'il prétend être
- **Appel réseau systématique** : l'API de jeux appelait `user-api` à chaque requête pour valider l'utilisateur, ce qui créait une dépendance forte et un point de latence

---

## 5.1 — Spring Security stateless et BCrypt

### Ajout des dépendances Maven

**Dans `user-api/pom.xml` et `api_java_3_5/api/pom.xml` :**

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JJWT — génération et validation des tokens -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**Pourquoi trois dépendances JJWT ?**
- `jjwt-api` : l'API publique (nécessaire à la compilation)
- `jjwt-impl` : l'implémentation (runtime uniquement)
- `jjwt-jackson` : sérialisation JSON des claims (runtime uniquement)

### Configuration stateless (user-api)

> 📁 `user-api/src/main/java/com/squaregames/user/common/security/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/{id}/valid").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**Points clés :**
- **`csrf.disable()`** : les API REST sont sans état, le CSRF ne s'applique pas
- **`SessionCreationPolicy.STATELESS`** : aucune session HTTP côté serveur
- **`requestMatchers(HttpMethod.POST, "/users").permitAll()`** : l'inscription est publique (mais protégée par `@Valid`)
- **`@EnableMethodSecurity`** : active `@PreAuthorize` sur les méthodes des contrôleurs
- **`frameOptions.sameOrigin()`** : autorise la console H2 en iframe (même origine)

### Modification de l'entité User

> 📁 `user-api/src/main/java/com/squaregames/user/user/domain/User.java`

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;              // ← ajouté : hashé avec BCrypt

    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";    // ← ajouté : ROLE_USER par défaut

    @Column(name = "created_at")
    private Instant createdAt;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public User(String name, String email, String password, String role) {
        this();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "ROLE_USER";
    }

    // Getters et setters pour password et role ajoutés
}
```

**Pourquoi `ROLE_USER` et pas juste `USER` ?**
Spring Security attend le préfixe `ROLE_` pour les autorisations. `@PreAuthorize("hasRole('ADMIN')")` cherche l'autorité `ROLE_ADMIN`.

---

## 5.2 — JWT : génération, validation et filtre

### JwtService

> 📁 `user-api/src/main/java/com/squaregames/user/common/security/JwtService.java`

```java
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        return parseToken(token).get("roles", List.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**Syntaxe décryptée :**
- **`@Value("${jwt.secret}")`** : lit la clé secrète depuis `application.properties`
- **`Keys.hmacShaKeyFor()`** : génère une clé HMAC-SHA256 à partir d'une chaîne. La clé doit faire au moins 256 bits (32 caractères).
- **`.subject(userId)`** : le "subject" du JWT = l'identifiant de l'utilisateur
- **`.claim("roles", roles)`** : claim personnalisé contenant la liste des rôles
- **`parseSignedClaims()`** : vérifie la signature ET l'expiration en une seule opération

### JwtAuthenticationFilter

> 📁 `user-api/src/main/java/com/squaregames/user/common/security/JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                String userId = jwtService.extractUserId(token);
                List<String> roles = jwtService.extractRoles(token);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Comment ça fonctionne :**
1. Le filtre intercepte **chaque requête HTTP**
2. Il lit l'entête `Authorization: Bearer <token>`
3. Il valide la signature et l'expiration du token
4. Il crée un objet `Authentication` (principal = `userId`, authorities = rôles)
5. Il stocke cet objet dans `SecurityContextHolder` → Spring Security sait qui est connecté

**Pourquoi `OncePerRequestFilter` ?**
Héritage qui garantit que le filtre s'exécute exactement une fois par requête, même en cas de forward/include interne.

### CustomUserDetailsService

> 📁 `user-api/src/main/java/com/squaregames/user/common/security/CustomUserDetailsService.java`

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getId(),          // principal = userId (pas email)
                user.getPassword(),    // mot de passe hashé (BCrypt)
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
```

**Pourquoi `user.getId()` comme username ?**
`Authentication.getName()` retourne ensuite le `userId`, ce qui est plus pratique pour les APIs (on travaille avec des IDs, pas des emails).

### AuthController — endpoint /auth/login

> 📁 `user-api/src/main/java/com/squaregames/user/auth/api/AuthController.java`

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            String userId = authentication.getName();
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié mais introuvable"));

            String token = jwtService.generateToken(userId, user.getEmail(), List.of(user.getRole()));

            return ResponseEntity.ok(new LoginResponse(token, userId, user.getEmail(), user.getRole()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
```

**Flux du login :**
1. Le client envoie `{email, password}`
2. `AuthenticationManager` utilise `CustomUserDetailsService` pour charger l'utilisateur
3. Il compare le mot de passe avec `BCryptPasswordEncoder.matches()`
4. Si OK → génération du JWT avec `userId`, `email`, `role`
5. Si KO → `401 Unauthorized`

---

## 5.3a — Champ role, @PreAuthorize et contrôle d'accès

### Modification du UserController

> 📁 `user-api/src/main/java/com/squaregames/user/user/api/UserController.java`

```java
@PostMapping
public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreationRequest request) {
    // Pas de @PreAuthorize — l'inscription est publique (permitAll dans SecurityConfig)
    User user = userService.createUser(request.name(), request.email(), request.password(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(user));
}

@GetMapping
@PreAuthorize("hasRole('ADMIN')")  // ← Seuls les admins peuvent lister tous les utilisateurs
public List<UserDto> getAllUsers() {
    return userService.getAllUsers().stream().map(this::toDto).toList();
}

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")  // ← Seuls les admins peuvent supprimer
public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}
```

**Syntaxe :**
- **`@PreAuthorize("hasRole('ADMIN')")`** : Spring vérifie le rôle **avant** d'appeler la méthode. Si le token ne contient pas `ROLE_ADMIN` → `403 Forbidden`.
- **`@EnableMethodSecurity`** (dans `SecurityConfig`) est **obligatoire** pour que ces annotations fonctionnent.

### GlobalExceptionHandler — préserver les codes HTTP

> 📁 `user-api/src/main/java/com/squaregames/user/common/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
```

**Pourquoi ce handler est critique :**
Sans lui, quand `UserServiceImpl.createUser` lève `ResponseStatusException(HttpStatus.CONFLICT)` pour un email dupliqué, Spring Security's `ExceptionTranslationFilter` intercepte l'exception et retourne `403 Forbidden` (car l'utilisateur n'est pas authentifié sur un endpoint `permitAll()`).

Le `@RestControllerAdvice` capture l'exception **avant** le filtre de sécurité et préserve le vrai code HTTP (`409 Conflict` ou `400 Bad Request`).

---

## 5.3b — JWT dans l'API de jeux, suppression de X-UserId

### Configuration stateless avec AuthenticationEntryPoint (api jeux)

> 📁 `api_java_3_5/api/src/main/java/com/squaregames/api/common/security/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/games/catalog").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // ← 401, pas 403
            }))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**Pourquoi un `AuthenticationEntryPoint` ?**
Par défaut, Spring Security retourne `403 Forbidden` pour une requête non authentifiée. Dans une API REST, le code correct est `401 Unauthorized` (identité manquante) plutôt que `403 Forbidden` (identité présente mais droits insuffisants).

### Modification du GameController

> 📁 `api_java_3_5/api/src/main/java/com/squaregames/api/game/api/GameController.java`

```java
@PostMapping
public GameDto createGame(@RequestBody GameCreationParams params) {
    return gameService.createGame(params, getCurrentUserId());  // ← plus de @RequestHeader("X-UserId")
}

@GetMapping
public Collection<GameDto> listGames() {
    return gameService.listGames(getCurrentUserId());
}

@PostMapping("/{gameId}/moves")
public GameDto playMove(@PathVariable UUID gameId, @RequestBody MoveRequest move) {
    return gameService.playMove(gameId, move, getCurrentUserId());
}

private String getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();  // ← le userId extrait du JWT par le filtre
}
```

**Changements majeurs :**
- Suppression de `@RequestHeader("X-UserId")` partout
- Le `userId` est extrait du `SecurityContext` (mis en place par `JwtAuthenticationFilter`)
- Plus d'appel réseau vers `user-api` — la validation est **locale**

### Suppression de UserValidator

Avant (itération 4) :
```java
private final UserValidator userValidator;

public GameDto createGame(GameCreationParams params, String userId) {
    userValidator.validate(userId);  // ← appel réseau vers user-api
    // ...
}
```

Après (itération 5) :
```java
// UserValidator supprimé — la validation est faite par le filtre JWT

public GameDto createGame(GameCreationParams params, String userId) {
    // Le userId provient du JWT déjà validé — pas besoin de re-vérifier
    // ...
}
```

**Avantages :**
- **Zéro appel réseau** : la validation JWT est locale (même clé secrète)
- **Moins de dépendances** : suppression de `RestUserValidator`, `UserValidator` interface
- **Plus rapide** : pas de latence HTTP inter-services

---

## Architecture complète des deux applications

### user-api (port 8081)

```
user-api/
├── pom.xml                                    ← + spring-boot-starter-security, JJWT
└── src/main/java/com/squaregames/user/
    ├── UserApiApplication.java
    ├── auth/api/
    │   ├── AuthController.java                  ← POST /auth/login
    │   ├── LoginRequest.java                    ← record { email, password }
    │   └── LoginResponse.java                   ← record { token, userId, email, role }
    ├── common/
    │   ├── config/
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java      ← @RestControllerAdvice (400, 409)
    │   └── security/
    │       ├── SecurityConfig.java              ← Stateless, CSRF off, permitAll rules
    │       ├── JwtService.java                  ← Génération et validation JWT
    │       ├── JwtAuthenticationFilter.java     ← Filtre Bearer token
    │       └── CustomUserDetailsService.java    ← UserDetailsService (BCrypt)
    └── user/
        ├── api/
        │   ├── UserController.java              ← + @PreAuthorize sur GET/DELETE
        │   └── dto/
        │       ├── UserCreationRequest.java     ← + password, role
        │       └── UserDto.java                 ← + role
        ├── application/
        │   ├── UserService.java
        │   ├── UserServiceImpl.java             ← + BCrypt hash, role
        │   └── UserDao.java                     ← + findByEmail, existsByEmail
        ├── domain/
        │   └── User.java                        ← + password, role fields
        └── infrastructure/
            └── JpaUserDao.java                  ← + findByEmail, existsByEmail
```

### api (jeux) (port 8080)

```
api_java_3_5/api/
├── pom.xml                                    ← + spring-boot-starter-security, JJWT
└── src/main/java/com/squaregames/api/
    ├── common/
    │   └── security/
    │       ├── SecurityConfig.java            ← + AuthenticationEntryPoint (401)
    │       ├── JwtService.java                ← Validation uniquement (même clé)
    │       └── JwtAuthenticationFilter.java  ← Extraction userId + rôles
    └── game/
        ├── api/
        │   └── GameController.java            ← - @RequestHeader("X-UserId"), + getCurrentUserId()
        └── application/
            └── GameServiceImpl.java            ← - UserValidator, - userValidator.validate()
```

---

## Configuration application.properties

### user-api

```properties
# JWT
jwt.secret=SquareGamesSecretKeyForJwtTokenSigningMustBe256BitsLong2024!
jwt.expiration-ms=86400000

# TestRestTemplate (désactiver basic auth auto)
spring.boot.testcontext.resttemplate.use-basic-auth=false
```

### api (jeux)

```properties
# JWT (même clé secrète que user-api)
jwt.secret=SquareGamesSecretKeyForJwtTokenSigningMustBe256BitsLong2024!
jwt.expiration-ms=86400000
```

---

## Tests mis à jour

### user-api

- **UserControllerIntegrationTest** : création d'utilisateurs avec mot de passe, test du login retournant un JWT, tests `@PreAuthorize` avec token admin
- **GlobalExceptionHandler** : garantit que les erreurs de validation (400) et les conflits (409) sont correctement propagés malgré Spring Security

### api (jeux)

- **GameControllerIntegrationTest** : remplacement total de `X-UserId` par `Authorization: Bearer <token>`. Les méthodes utilitaires `authHeaders()` et `generateToken()` factorisent la création des headers.
- **ConnectFourAndTaquinIntegrationTest** : même remplacement JWT pour les jeux ConnectFour et Taquin
- **JwtAuthContractTest** : tests de contrat vérifiant le comportement avec token valide, invalide, et absent
- **UserValidationContractTest** : réécrit pour tester la validation JWT locale (plus d'appel réseau WireMock)
- **GameServiceImplTest** : suppression des mocks `UserValidator`

---

## Points de vigilance sécurité

- **Mot de passe hashé** : `BCryptPasswordEncoder` avec force 10 (salt aléatoire)
- **Pas de secret codé en dur dans le code source** : la clé JWT est dans `application.properties`
- **Durée de vie limitée** : 24 heures pour limiter l'impact d'un token volé
- **Pas de session côté serveur** : chaque requête est autonome, impossible de détourner une session
- **Rôles dans le token** : le backend ne fait pas confiance à une base de données externe pour vérifier les droits
- **GlobalExceptionHandler** : préserve les codes HTTP d'erreur métier (400, 409) malgré Spring Security

---

## Livrables attendus

- ✅ `SecurityConfig.java` stateless avec CSRF disabled dans les deux applications
- ✅ `JwtService.java` — génération et validation des tokens (même clé secrète)
- ✅ `JwtAuthenticationFilter.java` — filtre Bearer token extrayant userId et rôles
- ✅ `CustomUserDetailsService.java` — UserDetailsService avec BCrypt
- ✅ `AuthController.java` — endpoint `/auth/login` retournant un JWT
- ✅ `LoginRequest.java` / `LoginResponse.java` — DTOs du login
- ✅ Champ `role` dans l'entité User avec valeur par défaut `ROLE_USER`
- ✅ `@PreAuthorize("hasRole('ADMIN')")` sur les endpoints sensibles
- ✅ `GlobalExceptionHandler.java` — préserve les codes HTTP 400 et 409
- ✅ `GameController.java` — extraction du userId via `SecurityContextHolder`
- ✅ Suppression complète de `X-UserId` et `UserValidator`
- ✅ `AuthenticationEntryPoint` retournant 401 dans l'api de jeux

---

## Exemples curl mis à jour

Les exemples de l'itération 4 utilisant `X-UserId` sont obsolètes. Voici les nouveaux exemples avec JWT.

### 1. Créer un utilisateur (public)

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@test.com","password":"pass123","role":"ROLE_ADMIN"}'
```

### 2. Se connecter et récupérer un JWT

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"pass123"}'
```

Réponse :
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "82e6a9eb-...",
  "email": "alice@test.com",
  "role": "ROLE_ADMIN"
}
```

### 3. Créer une partie (avec JWT)

```bash
# Remplace <token> par le JWT reçu à l'étape 2
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'
```

### 4. Lister ses parties (avec JWT)

```bash
curl http://localhost:8080/games \
  -H "Authorization: Bearer <token>"
```

### 5. Jouer un coup (avec JWT)

```bash
curl -X POST http://localhost:8080/games/{gameId}/moves \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"tokenName":"X","row":0,"col":0}'
```

> 📌 Le catalogue (`GET /games/catalog`) reste **public** — aucun JWT nécessaire.

---

## Commandes de commit suggérées

```bash
# user-api
git add user-api/src/main/java/com/squaregames/user/common/security/
git add user-api/src/main/java/com/squaregames/user/common/exception/
git add user-api/src/main/java/com/squaregames/user/auth/
git add user-api/src/test/java/com/squaregames/user/user/api/UserControllerIntegrationTest.java
git commit -m "feat(user-api): ajoute Spring Security, JWT et authentification"

# api (jeux)
git add api_java_3_5/api/src/main/java/com/squaregames/api/common/security/
git add api_java_3_5/api/src/main/java/com/squaregames/api/game/api/GameController.java
git add api_java_3_5/api/src/main/java/com/squaregames/api/game/application/GameServiceImpl.java
git add api_java_3_5/api/src/test/java/com/squaregames/api/game/api/
git commit -m "feat(api): remplace X-UserId par JWT, validation locale du token"
```

---

## Notes d'implémentation

### Clé secrète partagée

Les deux applications partagent la **même clé secrète** pour signer et valider les tokens :

```
SquareGamesSecretKeyForJwtTokenSigningMustBe256BitsLong2024!
```

Cette clé est définie dans `application.properties` des deux applications. Elle garantit qu'un token émis par `user-api` est accepté par `api`. En production, cette clé serait injectée via une variable d'environnement ou un gestionnaire de secrets (Vault, AWS Secrets Manager...).

### Pourquoi deux JwtAuthenticationFilter ?

Les filtres des deux applications ont la **même logique** (extraire le Bearer, parser le JWT, mettre l'Authentication dans le contexte) mais sont dans des packages différents. On pourrait factoriser dans une librairie commune, mais pour cet exercice la duplication est acceptable.

### Suppression de RestUserValidator

`RestUserValidator` et l'interface `UserValidator` ont été supprimés de l'api de jeux car la validation utilisateur est maintenant implicite : si le JWT est valide, l'utilisateur existe (il a été créé dans user-api à un moment donné).

### Console H2 et Spring Security

Avec Spring Security activé, la console H2 est bloquée par défaut car elle utilise des iframes. La ligne `.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))` dans `SecurityConfig` autorise les iframes depuis la même origine, ce qui permet d'accéder à `/h2-console`.

### TestRestTemplate et Spring Security

`TestRestTemplate` auto-configuré par Spring Boot tente parfois d'envoyer une authentification basique (Basic Auth) par défaut. La propriété `spring.boot.testcontext.resttemplate.use-basic-auth=false` dans `user-api/src/test/resources/application.properties` désactive ce comportement pour éviter les conflits avec le filtre JWT.
