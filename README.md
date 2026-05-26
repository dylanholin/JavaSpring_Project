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

### Étape 1 — Créer un utilisateur

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'
```

Réponse :
```json
{
  "id": "a1b2c3d4-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "Alice",
  "email": "alice@example.com",
  "createdAt": "2026-05-26T10:00:00Z"
}
```

> 📌 Note l'`id` — il sera utilisé comme `X-UserId` pour créer une partie.

---

### Étape 2 — Voir les jeux disponibles

```bash
curl http://localhost:8080/games/catalog
```

Jeux disponibles : `tictactoe`, `connect4`, `taquin`

---

### Étape 3 — Créer une partie

```bash
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -H "X-UserId: a1b2c3d4-xxxx-xxxx-xxxx-xxxxxxxxxxxx" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'
```

Réponse :
```json
{
  "id": "b2c3d4e5-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3,
  "status": "ONGOING",
  "currentPlayerId": "c3d4e5f6-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

> 📌 Note le `currentPlayerId` — c'est l'UUID du joueur dont c'est le tour. Depuis l'itération 4.2, cet UUID correspond à l'`id` de l'utilisateur qui a créé la partie (passé dans `X-UserId` à l'étape 3). Utilise-le comme `X-UserId` pour jouer un coup.

---

### Étape 4 — Voir les coups possibles

```bash
curl http://localhost:8080/games/b2c3d4e5-xxxx/moves
```

---

### Étape 5 — Jouer un coup

> ⚠️ Le `X-UserId` doit être le `currentPlayerId` retourné à l'étape 3.

```bash
curl -X POST http://localhost:8080/games/b2c3d4e5-xxxx/moves \
  -H "Content-Type: application/json" \
  -H "X-UserId: c3d4e5f6-xxxx-xxxx-xxxx-xxxxxxxxxxxx" \
  -d '{"tokenName":"X","row":0,"col":0}'
```

---

### Étape 6 — Voir l'état de la partie

```bash
curl http://localhost:8080/games/b2c3d4e5-xxxx
```

---

### Étape 7 — Lister mes parties

```bash
curl http://localhost:8080/games \
  -H "X-UserId: a1b2c3d4-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
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
├── api_java_3_5/api/     ← Application de jeux (port 8080)
│   ├── README.md
│   └── pom.xml
├── user-api/             ← Application utilisateurs (port 8081)
│   ├── README.md
│   └── pom.xml
├── explication3.md       ← Documentation pédagogique itération 3
├── explication4.md       ← Documentation pédagogique itération 4
└── suivi.md              ← Suivi de progression
```