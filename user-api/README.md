# API de Gestion des Utilisateurs

Application Spring Boot pour la gestion des utilisateurs (CRUD).

## Démarrage

```bash
# Avec Maven wrapper
./mvnw spring-boot:run

# Ou avec Maven installé
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8081**

## Configuration

- **Port** : 8081
- **Base de données** : H2 en mémoire (console disponible sur /h2-console)

## Endpoints

### CRUD Utilisateurs
- `POST /users` — Créer un utilisateur
- `GET /users/{id}` — Récupérer un utilisateur
- `DELETE /users/{id}` — Supprimer un utilisateur
- `GET /users/{id}/valid` — Vérifier si un utilisateur existe (retourne `true` ou `false`)

## Exemple d'utilisation

```bash
# Créer un utilisateur
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'

# Récupérer un utilisateur
curl http://localhost:8081/users/{id}

# Vérifier si un utilisateur existe
curl http://localhost:8081/users/{id}/valid
```

## Validation

- `name` : requis, non vide
- `email` : requis, format email valide, unique
