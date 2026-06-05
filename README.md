# SquareGames

Application de jeux de plateau (TicTacToe, ConnectFour, Taquin) développée en Java 21 avec Spring Boot 3.5, dans le cadre d'un apprentissage progressif des bonnes pratiques back-end.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![H2](https://img.shields.io/badge/Database-H2-0078D4)](https://h2database.com/)
[![Tests](https://img.shields.io/badge/tests-74%20passing-brightgreen)](docs/start_project.md)
[![Microservices](https://img.shields.io/badge/architecture-microservices-blue)](README.md#architecture)

## Sommaire

- [Architecture](#architecture)
  - [Organisation interne : architecture hexagonale (Ports & Adapters)](#organisation-interne--architecture-hexagonale-ports--adapters)
- [Prérequis](#prérequis)
- [Configuration](#configuration)
- [Base de données](#base-de-données)
- [Jouer une partie : guide complet](#jouer-une-partie--guide-complet)
- [Console H2 (débogage)](#console-h2-débogage)
- [Structure du projet](#structure-du-projet)

## Architecture

Ce projet est composé de **deux applications Spring Boot indépendantes** (architecture microservices) :

| Application | Port | Rôle |
|-------------|------|------|
| `api_java_3_5/api` | **8080** | Gestion des parties de jeux (TicTacToe, ConnectFour, Taquin) |
| `user-api` | **8081** | Gestion des utilisateurs (CRUD, validation) |

Depuis l'itération 5, l'authentification repose sur **JWT** (JSON Web Token) : le client se connecte sur `user-api` pour obtenir un token, puis le présente à chaque requête via `Authorization: Bearer <token>`. L'app de jeux valide le token **localement** sans appel réseau vers `user-api`.

### Organisation interne : architecture hexagonale (Ports & Adapters)

Chaque feature (`game/`, `user/`) n'est **pas** organisée en couches techniques classiques (`controller` / `service` / `dao`), mais selon le rôle de chaque classe dans le sens des dépendances. C'est l'**architecture hexagonale** (aussi appelée *Ports & Adapters* ou *Clean Architecture*).

```
game/
├── api/             ← Adapter d'ENTRÉE (REST) : GameController + DTO
├── application/     ← Cœur métier : services + PORTS (interfaces)
│   ├── GameService / GameServiceImpl    ← logique métier
│   ├── GameDao.java                     ← PORT (interface)
│   ├── UserValidator.java               ← PORT (interface) — @Deprecated (itération 5)
│   └── GamePlugin.java
├── domain/          ← Entités JPA (modèle persistant)
└── infrastructure/  ← Adapters de SORTIE (détails techniques)
    ├── JpaGameDao / JdbcGameDao (@Deprecated) / InMemoryGameDao (@Deprecated)  ← implémentent GameDao
    └── RestUserValidator (@Deprecated)              ← implémente UserValidator (itération 4)
```

**Pourquoi le DAO n'est-il pas regroupé dans un seul package `dao` ?**

Le DAO est volontairement coupé en deux :

- L'**interface** `GameDao` est dans `application/`. C'est un **port** : le métier déclare *ce dont il a besoin* (persister, lire, supprimer une partie), sans connaître la technologie.
- Les **implémentations** (`JpaGameDao`, `JdbcGameDao`, `InMemoryGameDao`) sont dans `infrastructure/`. Ce sont des **adapters** : des détails techniques interchangeables.

Conséquence : `GameServiceImpl` ne dépend **jamais** de JPA ni de SQL. On a pu passer de `InMemoryGameDao` → `JdbcGameDao` → `JpaGameDao` (itérations 3.2 à 3.4) sans modifier une seule ligne du service. Le choix de l'implémentation active se fait via `@Primary` sur `JpaGameDao`.

**Pourquoi `RestUserValidator` est-il dans `infrastructure/` ?**

Même logique :

- `UserValidator` (interface, dans `application/`) = le métier dit « j'ai besoin de valider un utilisateur ».
- `RestUserValidator` (dans `infrastructure/`) = le *comment* technique : un appel HTTP sortant via `RestClient`. Tout appel réseau vers un système externe est, par nature, de l'infrastructure. Il est remplaçable (on pourrait écrire un `LocalUserValidator` pour les tests, par exemple).

> 📌 **Évolution** : dans l'itération 5, `UserValidator` et `RestUserValidator` ont été supprimés et remplacés par un mécanisme JWT. La validation utilisateur est maintenant implicite : si le token JWT est valide, l'utilisateur existe (il a été créé dans user-api). L'app de jeux n'appelle plus user-api à chaque requête.

**Avantages de ce découpage**

- Le cœur métier (`application/`) est testable en isolation : on mocke les ports (`UserValidator`, `GameDao`).
- Les dépendances pointent toujours **vers le métier**, jamais l'inverse (règle de dépendance de la Clean Architecture).
- Ajouter un nouveau jeu = ajouter un `GamePlugin`, sans toucher au contrôleur ni au DAO.

---

## Prérequis

- **Java 21+** (JDK requis)
- **Maven** (ou utiliser le wrapper `./mvnw` inclus dans chaque application)
- Aucune base de données externe nécessaire : H2 embarqué en fichier est utilisé par défaut

> 📌 **Installation et démarrage** : lire impérativement [docs/start_project.md](docs/start_project.md) avant d'utiliser le projet. Les instructions pas-à-pas couvrent Fedora, Ubuntu et Windows (testé sur Fedora Workstation et Windows 11).

---

## Configuration

La configuration se fait via `application.properties` avec des profils Spring. Aucun fichier `.env` n'est requis.

### Application de jeux (`api_java_3_5/api/src/main/resources/application.properties`)

| Propriété | Exemple | Description |
|-----------|---------|-------------|
| `spring.profiles.active` | `h2` | Profil actif : `h2` (défaut) ou `mysql` (production) |
| `jwt.secret` | `SquareGamesSecret...` | Clé secrète partagée pour valider les JWT (doit être identique dans user-api) |
| `game.tictactoe.default-player-count` | `2` | Nombre de joueurs par défaut pour TicTacToe |
| `game.tictactoe.default-board-size` | `3` | Taille de grille par défaut pour TicTacToe |
| `game.connectfour.default-player-count` | `2` | Nombre de joueurs par défaut pour ConnectFour |
| `game.connectfour.default-board-size` | `7` | Taille de grille par défaut pour ConnectFour |
| `game.taquin.default-player-count` | `1` | Nombre de joueurs par défaut pour le Taquin |
| `game.taquin.default-board-size` | `4` | Taille de grille par défaut pour le Taquin (grille 4x4) |

### Application utilisateurs (`user-api/src/main/resources/application.properties`)

| Propriété | Exemple | Description |
|-----------|---------|-------------|
| `server.port` | `8081` | Port du service utilisateurs |
| `spring.datasource.url` | `jdbc:h2:file:./data/userdb` | URL de la base H2 (fichier) |
| `spring.datasource.username` | `sa` | Identifiant base de données |
| `spring.datasource.password` | (vide) | Mot de passe base de données |
| `jwt.secret` | `SquareGamesSecret...` | Clé secrète pour signer les JWT (doit être identique dans api) |
| `jwt.expiration` | `86400000` | Durée de validité du token en millisecondes (24h) |

---

## Base de données

Les deux applications utilisent **H2 en mode fichier** par défaut (profil `h2`). Les données survivent au redémarrage.

| Application | JDBC URL | Fichier de données |
|-------------|----------|--------------------|
| App de jeux | `jdbc:h2:file:./data/squaregames` | `api_java_3_5/api/data/squaregames.mv.db` |
| user-api | `jdbc:h2:file:./data/userdb` | `user-api/data/userdb.mv.db` |

- **Schéma** : géré automatiquement par Hibernate (`ddl-auto=update`). Pas de scripts SQL ni de migrations Flyway/Liquibase.
- **Console H2** : accessible pour le débogage (voir section Console H2 ci-dessous).
- **Profil MySQL** : un template `application-mysql.properties` existe pour la production. Activer avec `--spring.profiles.active=mysql`.
- Les dossiers `data/` sont dans `.gitignore` et sont recréés automatiquement par Hibernate au premier démarrage.

---

## Jouer une partie : guide complet

> ⚠️ **Les deux applications doivent être démarrées** avant de commencer (voir [docs/start_project.md](docs/start_project.md)).
>
> **Authentification** : depuis l'itération 5, toutes les requêtes vers l'app de jeux nécessitent un token JWT dans l'entête `Authorization: Bearer <token>`.
>
> **Persistance** : les parties de jeux et les utilisateurs survivent au redémarrage (H2 fichier pour les deux applications).

### Étape 1 : Créer un compte utilisateur

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"monMotDePasse"}'
```

Exemple de réponse :
```json
{
  "id": "3c9eabbf-2bba-4c86-a453-98ee8cc101c4",
  "name": "Alice",
  "email": "alice@example.com",
  "createdAt": "2026-05-26T10:00:00Z"
}
```

### Étape 2 : Se connecter pour obtenir un JWT

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"monMotDePasse"}'
```

Exemple de réponse :
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIzYzllYWJiZi..."
}
```

> 📌 **Copie le champ `token`** : tu devras l'envoyer dans chaque requête suivante.

### Étape 3 : Voir les jeux disponibles

Le catalogue est public (pas de JWT requis) :

```bash
curl http://localhost:8080/games/catalog
```

Jeux disponibles : `tictactoe`, `connect4`, `15 puzzle`

### Étape 4 : Créer une partie

Remplace `{TOKEN}` par le token JWT copié à l'étape 2 :

```bash
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'
```

Exemple de réponse :
```json
{
  "id": "24f70b09-6104-4baa-97bf-1393959513fa",
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3,
  "status": "ONGOING",
  "currentPlayerId": "3c9eabbf-2bba-4c86-a453-98ee8cc101c4"
}
```

> 📌 **Copie le champ `id`** : c'est le `GAME_ID`, tu en auras besoin pour les coups.

### Étape 5 : Voir les coups possibles

```bash
curl http://localhost:8080/games/{GAME_ID}/moves \
  -H "Authorization: Bearer {TOKEN}"
```

### Étape 6 : Jouer un coup

```bash
curl -X POST http://localhost:8080/games/{GAME_ID}/moves \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{"tokenName":"X","row":0,"col":0}'
```

> ⚠️ Si le token JWT ne correspond pas au `currentPlayerId` de la partie, la réponse est `403 Forbidden`.

### Étape 7 : Voir l'état de la partie

```bash
curl http://localhost:8080/games/{GAME_ID} \
  -H "Authorization: Bearer {TOKEN}"
```

### Étape 8 : Lister mes parties

```bash
curl http://localhost:8080/games \
  -H "Authorization: Bearer {TOKEN}"
```

---

## Console H2 (débogage)

Permet de visualiser les données en base directement dans le navigateur.

| Application | URL | JDBC URL | Identifiants |
|-------------|-----|----------|--------------|
| App de jeux | http://localhost:8080/h2-console | `jdbc:h2:file:./data/squaregames` | user : `sa`, password : (vide) |
| user-api | http://localhost:8081/h2-console | `jdbc:h2:file:./data/userdb` | user : `sa`, password : (vide) |

---

## Structure du projet

> 📌 Documentation pédagogique : consulter le dossier `docs/` pour les explications détaillées de chaque itération.

```
JavaSpring_Project/
├── api_java_3_5/api/                 ← Application de jeux (port 8080)
│   ├── src/main/java/com/squaregames/api/
│   │   ├── ApiApplication.java       ← Classe principale
│   │   ├── HeartbeatController.java  ← Endpoint /heartbeat (itération 1)
│   │   ├── HeartbeatSensor.java      ← Interface capteur heartbeat
│   │   ├── RandomHeartbeat.java      ← Implémentation aléatoire du capteur
│   │   ├── common/                   ← Transverse : configuration, sécurité
│   │   │   └── security/
│   │   │       ├── SecurityConfig.java           ← Spring Security stateless + JWT filter
│   │   │       ├── JwtService.java               ← Validation des tokens JWT
│   │   │       └── JwtAuthenticationFilter.java ← Extraction userId depuis Bearer
│   │   └── game/                     ← Feature "jeu" (organisée par couches)
│   │       ├── api/                  ← Couche REST (contrôleurs + DTO)
│   │       │   ├── GameController.java          ← CRUD parties + mouvements
│   │       │   ├── GameCatalogController.java    ← Catalogue des jeux disponibles
│   │       │   └── dto/
│   │       │       ├── CatalogEntryDto.java      ← Entrée du catalogue (nom, description)
│   │       │       ├── GameCreationParams.java   ← Paramètres de création d'une partie
│   │       │       ├── GameDto.java              ← Représentation d'une partie
│   │       │       ├── MoveRequest.java         ← Requête pour jouer un coup
│   │       │       ├── PositionDto.java         ← Position (ligne, colonne)
│   │       │       └── TokenMovesDto.java       ← Coups possibles pour un token
│   │       ├── application/          ← Couche service + plugins
│   │       │   ├── GameService.java             ← Interface du service métier
│   │       │   ├── GameServiceImpl.java         ← Implémentation du service
│   │       │   ├── GamePlugin.java              ← Interface commune des plugins de jeu
│   │       │   ├── TicTacToePlugin.java         ← Plugin TicTacToe
│   │       │   ├── ConnectFourPlugin.java       ← Plugin ConnectFour
│   │       │   ├── TaquinPlugin.java            ← Plugin Taquin (15 puzzle)
│   │       │   ├── GameCatalog.java             ← Interface du catalogue de jeux
│   │       │   ├── GameCatalogImpl.java         ← Implémentation du catalogue
│   │       │   └── GameDao.java                ← Interface du DAO
│   │       ├── domain/               ← Modèle métier (entités JPA, repositories)
│   │       │   ├── GameEntity.java              ← Entité JPA d'une partie
│   │       │   ├── GameEntityRepository.java   ← Repository Spring Data JPA
│   │       │   └── GameTokenEntity.java         ← Entité JPA d'un token sur le plateau
│   │       └── infrastructure/       ← Adapters techniques (DAOs, clients externes)
│   │           ├── InMemoryGameDao.java          ← DAO en mémoire (ne survit pas au redémarrage)
│   │           ├── JdbcGameDao.java              ← DAO JDBC (SQL explicite, limitation d'état)
│   │           ├── JpaGameDao.java               ← DAO JPA (@Primary, persistance complète)
│   │           ├── ConnectFourStateAdapter.java  ← Normalise les positions ConnectFour pour reconstruction
│   │           └── GameStateWrapper.java         ← Préserve id et currentPlayerId en fallback
│   ├── src/main/resources/
│   │   ├── application.properties        ← Config principale (profil h2 par défaut)
│   │   ├── application-h2.properties    ← Profil H2 fichier (développement)
│   │   └── application-mysql.properties ← Profil MySQL (production, template)
│   ├── src/test/java/...              ← Tests unitaires et d'intégration
│   ├── README.md
│   └── pom.xml
├── user-api/                         ← Application utilisateurs (port 8081)
│   ├── src/main/java/com/squaregames/user/
│   │   ├── UserApiApplication.java   ← Classe principale
│   │   ├── auth/                     ← Feature "authentification" (itération 5)
│   │   │   ├── api/
│   │   │   │   ├── AuthController.java         ← POST /auth/login (retourne JWT)
│   │   │   │   ├── LoginRequest.java           ← Payload de connexion
│   │   │   │   └── LoginResponse.java          ← Contient le token JWT
│   │   ├── common/                   ← Transverse : configuration, exceptions, sécurité
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java ← Préserve codes HTTP 400/409 malgré Spring Security
│   │   │   └── security/
│   │   │       ├── SecurityConfig.java         ← Spring Security stateless + @EnableMethodSecurity
│   │   │       ├── JwtService.java             ← Génération et validation des tokens JWT
│   │   │       ├── JwtAuthenticationFilter.java ← Extraction userId + rôles depuis Bearer
│   │   │       └── CustomUserDetailsService.java ← Charge les utilisateurs depuis le DAO
│   │   └── user/                     ← Feature "utilisateur"
│   │       ├── api/                  ← Couche REST
│   │       │   ├── UserController.java          ← CRUD utilisateurs + validation
│   │       │   └── dto/
│   │       │       ├── UserCreationRequest.java ← Payload de création d'utilisateur
│   │       │       └── UserDto.java             ← Représentation d'un utilisateur
│   │       ├── application/          ← Couche service
│   │       │   ├── UserService.java             ← Interface du service utilisateur
│   │       │   ├── UserServiceImpl.java         ← Implémentation du service
│   │       │   └── UserDao.java                ← Interface du DAO utilisateur
│   │       ├── domain/               ← Modèle métier
│   │       │   ├── User.java                    ← Entité métier utilisateur (password, role)
│   │       │   └── UserRepository.java         ← Repository Spring Data JPA
│   │       └── infrastructure/       ← Couche persistance
│   │           └── JpaUserDao.java             ← Implémentation JPA du DAO
│   ├── src/test/java/...              ← Tests
│   ├── README.md
│   └── pom.xml
├── cda-java-spring-game-engine-main/  ← Moteur de jeu (bibliothèque, à installer en local)
├── img/                              ← Captures d'écran du projet
├── AGENTS.md                          ← Règles de travail pour l'IA
├── docs/                              ← Documentation pédagogique
│   ├── explication.md                  ← Itération 1
│   ├── explication2.md                 ← Itération 2
│   ├── explication3.md                 ← Itération 3 (persistance)
│   ├── explication4.md                 ← Itération 4 (utilisateurs et sécurité)
│   ├── explication5.md                 ← Itération 5 (sécurisation JWT et Spring Security)
│   ├── audit-technique.md              ← Audit technique du projet
│   ├── lexique-concepts.md            ← Lexique des concepts Spring Boot
│   ├── start_project.md                ← Guide de démarrage (Fedora, Ubuntu, Windows)
│   └── suivi.md                        ← Suivi de progression
└── README.md                          ← Ce fichier
```