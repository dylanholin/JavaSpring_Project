# Comment jouer une partie

Guide pas-à-pas basé sur l'API existante (itération 2).

> 📁 Racine Java : `api_java_3_5/api/src/main/java/com/squaregames/api/game/`

---

## 0. Démarrer l'application

Avant de tester les endpoints, il faut lancer le serveur Spring Boot.

```bash
cd /home/user/Documents/JavaSpring_Project/api_java_3_5/api
./mvnw spring-boot:run
```

Ou depuis ton IDE : clique droit sur `ApiApplication.java` → Run.

Le serveur démarre sur `http://localhost:8080` par défaut. Tu verras un log comme :

```
Started ApiApplication in X.xxx seconds
```

Une fois démarré, laisse le terminal ouvert — le serveur tourne en arrière-plan.

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

> ⚠️ **Important** : remplace `{id}` par l'UUID réel retourné par l'étape 1.
>
> Exemple : si l'UUID est `a2ac5ed2-7761-41d2-a794-eb8e3b919252`, l'URL devient :
> `http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves`

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

> ⚠️ Remplace `{id}` par l'UUID réel de ta partie.

## 4. Voir l'état de la partie

```http
GET http://localhost:8080/games/{id}
```

> ⚠️ Remplace `{id}` par l'UUID réel de ta partie.

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

## Exemple complet avec curl

Voici une séquence complète de test avec `curl` :

```bash
# 1. Créer une partie
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'

# Réponse : {"id":"a2ac5ed2-7761-41d2-a794-eb8e3b919252",...}

# 2. Voir les coups possibles (remplace l'UUID par celui reçu)
curl http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves

# 3. Jouer un coup X en (0,0)
curl -X POST http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves \
  -H "Content-Type: application/json" \
  -d '{"tokenName":"X","row":0,"col":0}'

# 4. Jouer un coup 0 en (1,1)
curl -X POST http://localhost:8080/games/a2ac5ed2-7761-41d2-a794-eb8e3b919252/moves \
  -H "Content-Type: application/json" \
  -d '{"tokenName":"0","row":1,"col":1}'
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
