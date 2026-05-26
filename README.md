# SquareGames — Projet Spring Boot

Application de jeux de plateau multi-joueurs développée en Java avec Spring Boot, dans le cadre d'un apprentissage progressif des bonnes pratiques back-end.

## Architecture

Ce projet est composé de **deux applications Spring Boot indépendantes** :

| Application | Port | Rôle |
|-------------|------|------|
| `api_java_3_5/api` | **8080** | Gestion des parties de jeux (TicTacToe, ConnectFour, Taquin) |
| `user-api` | **8081** | Gestion des utilisateurs (CRUD) |

Les deux applications communiquent via REST : l'app de jeux appelle user-api pour valider l'identité du joueur (`X-UserId`).

---

## Démarrage

### Prérequis

- Java 21+
- Maven (ou utiliser le wrapper `./mvnw` inclus)

### 1. Démarrer user-api (port 8081)

```bash
cd user-api
./mvnw spring-boot:run
```

### 2. Démarrer l'app de jeux (port 8080)

```bash
cd api_java_3_5/api
./mvnw spring-boot:run
```

> ⚠️ Démarrer **user-api en premier** — l'app de jeux en a besoin pour valider les utilisateurs.

---

## Jouer une partie — Guide complet

> ⚠️ **Les deux applications doivent être démarrées** avant de commencer (voir section Démarrage).
> ⚠️ **La base de données est en mémoire** : toutes les données sont perdues à chaque redémarrage. Il faut recréer les utilisateurs à chaque fois.

---

### Étape 1 — Créer un utilisateur

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'
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

> 📌 **Copie le champ `id`** de cette réponse — c'est l'identifiant utilisateur. Tu en auras besoin à l'étape 3.

---

### Étape 2 — Voir les jeux disponibles

```bash
curl http://localhost:8080/games/catalog
```

Jeux disponibles : `tictactoe`, `connect4`, `15 puzzle`

---

### Étape 3 — Créer une partie

Remplace `{USER_ID}` par le champ `id` copié à l'étape 1 :

```bash
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -H "X-UserId: {USER_ID}" \
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

> 📌 **Copie le champ `id`** → c'est le `GAME_ID`, tu en auras besoin pour les coups.
> 📌 **Copie le champ `currentPlayerId`** → c'est l'UUID du joueur dont c'est le tour. Il doit correspondre à ton `USER_ID` pour que tu puisses jouer.

---

### Étape 4 — Voir les coups possibles

Remplace `{GAME_ID}` par le champ `id` copié à l'étape 3 :

```bash
curl http://localhost:8080/games/{GAME_ID}/moves
```

---

### Étape 5 — Jouer un coup

Remplace `{GAME_ID}` par le champ `id` et `{CURRENT_PLAYER_ID}` par le champ `currentPlayerId` copiés à l'étape 3 :

```bash
curl -X POST http://localhost:8080/games/{GAME_ID}/moves \
  -H "Content-Type: application/json" \
  -H "X-UserId: {CURRENT_PLAYER_ID}" \
  -d '{"tokenName":"X","row":0,"col":0}'
```

> ⚠️ Si `X-UserId` ne correspond pas au `currentPlayerId` → `403 Forbidden`

---

### Étape 6 — Voir l'état de la partie

```bash
curl http://localhost:8080/games/{GAME_ID}
```

---

### Étape 7 — Lister mes parties

```bash
curl http://localhost:8080/games \
  -H "X-UserId: {USER_ID}"
```

---

## Codes HTTP

| Code | Signification |
|------|---------------|
| `200` | Succès |
| `201` | Ressource créée |
| `400` | Header `X-UserId` manquant ou corps invalide |
| `403` | Utilisateur inconnu ou ce n'est pas ton tour |
| `404` | Partie ou utilisateur introuvable |

---

## Console H2 (débogage)

Permet de visualiser les données en base directement dans le navigateur.

| Application | URL | JDBC URL |
|-------------|-----|----------|
| App de jeux | http://localhost:8080/h2-console | `jdbc:h2:mem:squaregames` |
| user-api | http://localhost:8081/h2-console | `jdbc:h2:mem:userdb` |

Identifiants : user `sa`, password vide.

> ⚠️ Les données sont perdues à chaque redémarrage (base en mémoire).

---

## Lancer les tests

```bash
# Tests de l'app de jeux
cd api_java_3_5/api
./mvnw test

# Tests de user-api
cd user-api
./mvnw test
```

---

## Structure du projet

```
JavaSpring_Project/
├── api_java_3_5/api/                 ← Application de jeux (port 8080)
│   ├── src/main/java/com/squaregames/api/
│   │   ├── ApiApplication.java       ← Classe principale
│   │   ├── common/                   ← Transverse (config, exceptions, sécurité)
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   └── security/
│   │   └── game/                     ← Feature "jeu" (organisée par couches)
│   │       ├── api/                  ← Couche REST (contrôleurs + DTO)
│   │       │   ├── GameController.java
│   │       │   ├── GameCatalogController.java
│   │       │   └── dto/
│   │       ├── application/          ← Couche service + plugins
│   │       │   ├── GameService.java
│   │       │   ├── GamePlugin.java
│   │       │   ├── TicTacToePlugin.java
│   │       │   ├── ConnectFourPlugin.java
│   │       │   ├── TaquinPlugin.java
│   │       │   ├── GameCatalog.java
│   │       │   └── UserValidator.java
│   │       ├── domain/               ← Modèle métier (entités JPA, repositories)
│   │       │   ├── GameEntity.java
│   │       │   ├── GameEntityRepository.java
│   │       │   └── GameTokenEntity.java
│   │       └── infrastructure/       ← Adapters techniques (JPA, clients externes)
│   │           ├── JdbcGameDao.java
│   │           ├── JpaGameDao.java
│   │           └── RestUserValidator.java
│   ├── src/test/java/...              ← Tests unitaires et d'intégration
│   ├── README.md
│   └── pom.xml
├── user-api/                         ← Application utilisateurs (port 8081)
│   ├── src/main/java/com/squaregames/user/
│   │   ├── UserApiApplication.java   ← Classe principale
│   │   └── user/                     ← Feature "utilisateur"
│   │       ├── api/                  ← Couche REST
│   │       │   ├── UserController.java
│   │       │   └── dto/
│   │       ├── application/          ← Couche service
│   │       │   ├── UserService.java
│   │       │   └── UserDao.java
│   │       ├── domain/               ← Modèle métier
│   │       │   ├── User.java
│   │       │   └── UserRepository.java
│   │       └── infrastructure/       ← Couche persistance
│   │           └── JpaUserDao.java
│   ├── src/test/java/...              ← Tests
│   ├── README.md
│   └── pom.xml
├── cda-java-spring-game-engine-main/  ← Moteur de jeu externe (bibliothèque)
├── AGENTS.md                          ← Règles de travail pour l'IA
├── explication.md                     ← Documentation pédagogique itération 1
├── explication2.md                    ← Documentation pédagogique itération 2
├── explication3.md                    ← Documentation pédagogique itération 3
├── explication4.md                    ← Documentation pédagogique itération 4
├── explication5.md                    ← Documentation pédagogique itération 5
├── suivi.md                           ← Suivi de progression
└── README.md                          ← Ce fichier
```