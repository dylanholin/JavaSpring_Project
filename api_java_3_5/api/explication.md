# Projet Square Games - Itération 1

## 🎯 Objectif du projet
Créer une application Spring Boot qui expose un endpoint `GET /heartbeat`. Cet endpoint renverra une valeur aléatoire entre 40 et 230. 

L'objectif principal n'est pas juste de faire fonctionner la route, mais de comprendre la mécanique interne de Spring Boot, en particulier l'**Injection de Dépendances**.

## 🧠 Logique et Concepts clés
- **Inversion de Contrôle (IoC) / Injection de Dépendances :** Au lieu d'instancier manuellement des objets (avec `new`), on demande à Spring de fournir (injecter) objets nécessaires au bon fonctionnement de l'application.
- **Bean Spring :** Un objet qui est instancié, assemblé et géré par le conteneur Spring.
- **Le Component Scan :** Au démarrage, Spring parcourt le code à la recherche d'annotations de "stéréotype" pour créer les Beans correspondants.
- **Annotations importantes :**
  - `@RestController` : Indique que la classe gère des requêtes HTTP et renvoie directement la donnée.
  - `@Service` : Indique que la classe contient de la logique métier (c'est un bean qui pourra être injecté ailleurs).
  - `@Autowired` : Demande à Spring d'injecter une dépendance (un Bean).

## 🚀 Démarche pour l'Itération 1
1. **Initialisation (Fait) :** Création du projet Maven avec Java 21 et Spring Web.
2. **Démarrage à vide (En cours) :** Lancer l'application pour vérifier que le serveur tourne.
3. **Création du contrat :** Créer une interface `HeartbeatSensor` qui définit une méthode `int get()`.
4. **Création du contrôleur (avec erreur volontaire) :** Créer `HeartbeatController` qui expose `GET /heartbeat` et demande l'injection de `HeartbeatSensor`. Comme aucune implémentation de l'interface n'existe encore, Spring va échouer au démarrage, comme prévu dans l'exercice.
5. **Résolution de l'erreur :** Créer la classe `RandomHeartbeat` annotée `@Service` qui implémente `HeartbeatSensor`.
6. **Validation :** Redémarrer l'application et tester avec Bruno, Postman ou un navigateur.
