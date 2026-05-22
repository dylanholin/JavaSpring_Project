# Suivi du projet SquareGames

Dernière mise à jour : 22/05/2026

---

## Itération 1 — Premier endpoint

| Étape | Statut |
|---|---|
| 1.1 — Création du projet Maven | ✅ |
| 1.2 — Premier endpoint `/heartbeat` | ✅ |
| 1.3 — Test de l'endpoint | ✅ |

---

## Itération 2 — API jeu de plateau

| Étape | Statut |
|---|---|
| 2.1 — Mise en place du projet Maven + GameCatalog | ✅ |
| 2.2 — Conception de l'API REST | ✅ |
| 2.3 — Implémentation de l'API (Controller/Service/DTO) | ✅ |
| 2.4 — Injection de valeurs et internationalisation (Plugins, @Value, i18n) | ✅ |

---

## Itération 3 — Persistance (DAO, JDBC, JPA)

| Étape | Statut |
|---|---|
| 3.1 — Comprendre le pattern DAO (lecture) | ❌ |
| 3.2 — Mise en place du DAO en mémoire (refactoring) | ✅ |
| 3.3 — Implémentation du DAO avec JDBC (SQL explicite) | ✅ |
| 3.4 — Implémentation du DAO avec JPA / Spring Data (SQL automatisé) | ✅ |
| 3.5 — Gérer plusieurs sources de données avec profils Spring (bonus) | ✅ |

---

## Tests — Golden Master (bonus)

| Étape | Statut |
|---|---|
| Tests d'intégration API (GameController) | ✅ |
| Tests catalogue (GameCatalogController) | ✅ |
| Tests unitaires Service (GameServiceImpl) | ✅ |

## Itération 4 — Utilisateurs & Sécurité

| Étape | Statut |
|---|---|
| 4.1 — Création de l'API de gestion des utilisateurs | ✅ |
| 4.2 — Modification de l'API de jeux (X-UserId, RestClient) | ❌ |
