# API de Jeux de Plateau

Application Spring Boot pour la gestion de parties de jeux de plateau (TicTacToe, ConnectFour, Taquin).

## Démarrage

```bash
# Avec Maven wrapper
./mvnw spring-boot:run

# Ou avec Maven installé
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080**

## Configuration

Le port et la base de données sont configurés dans `src/main/resources/application.properties` :

- **Port** : 8080
- **Profil par défaut** : h2 (base en mémoire)
- **Service utilisateurs** : http://localhost:8081

Pour utiliser MySQL :
```bash
./mvnw spring-boot:run -Dspring.profiles.active=mysql
```

## Endpoints

### Catalogue de jeux
- `GET /games/catalog` — Liste les types de jeux disponibles

### Gestion des parties
- `POST /games` — Créer une partie (header `X-UserId` requis)
- `GET /games` — Lister les parties du joueur (header `X-UserId` requis)
- `GET /games/{id}` — Récupérer une partie
- `GET /games/{id}/moves` — Récupérer les mouvements possibles
- `POST /games/{id}/moves` — Jouer un coup (header `X-UserId` requis)

## Exemple d'utilisation

```bash
# Créer une partie TicTacToe
curl -X POST http://localhost:8080/games \
  -H "Content-Type: application/json" \
  -H "X-UserId: user-123" \
  -d '{"gameType":"tictactoe","playerCount":2,"boardSize":3}'

# Lister mes parties
curl http://localhost:8080/games -H "X-UserId: user-123"

# Jouer un coup
curl -X POST http://localhost:8080/games/{gameId}/moves \
  -H "Content-Type: application/json" \
  -H "X-UserId: user-123" \
  -d '{"tokenName":"X","row":0,"col":0}'
```

## Dépendances

Le service **user-api** doit être démarré sur http://localhost:8081 pour la validation des utilisateurs.
