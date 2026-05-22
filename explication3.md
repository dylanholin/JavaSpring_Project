# Comment jouer une partie

Guide pas-à-pas basé sur l'API existante (itération 2).

> 📁 Racine Java : `api_java_3_5/api/src/main/java/com/squaregames/api/game/`

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

## 4. Voir l'état de la partie

```http
GET http://localhost:8080/games/{id}
```

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

## Résumé du flux

```
POST   /games              → crée une partie
GET    /games/{id}/moves   → coups possibles
POST   /games/{id}/moves   → joue un coup
GET    /games/{id}         → état de la partie
GET    /games              → toutes les parties
GET    /games/catalog      → jeux disponibles
```
