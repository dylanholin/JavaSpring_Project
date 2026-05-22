# Comprendre Spring Boot pour bien débuter

Spring Boot sert à accélérer le développement d'applications Java en fournissant une structure prête à l'emploi, une configuration automatique et, dans beaucoup de cas, un serveur web embarqué comme Tomcat.  
L'idée la plus importante à retenir est simple : le framework prend en charge une partie de la création et de l'assemblage des objets pour que le développeur se concentre davantage sur la logique métier.

## Spring et Spring Boot

Spring est le framework de base, tandis que Spring Boot est une couche de simplification construite par-dessus pour démarrer plus vite et configurer moins de choses à la main.  
Spring Boot réduit la configuration manuelle grâce à l'auto-configuration, au composant scan et à des dépendances prêtes à l'emploi comme `spring-boot-starter-web`.

## La logique centrale

Le coeur de Spring repose sur l'IoC, pour *Inversion of Control* : ce n'est plus le code applicatif qui pilote toute la création des objets, c'est le conteneur Spring qui les instancie et les gère.  
Cette logique mène naturellement à l'injection de dépendances : lorsqu'une classe a besoin d'une autre classe, Spring peut lui fournir automatiquement la bonne instance au lieu d'obliger le développeur à écrire `new` partout.  
Un bean Spring est justement un objet géré par ce conteneur IoC.

## Les annotations à connaître

`@SpringBootApplication` est l'annotation de départ la plus importante : elle regroupe notamment la configuration, l'auto-configuration et le scan des composants.  
`@RestController` sert à exposer des endpoints HTTP, `@Service` à déclarer une classe de logique métier, `@Repository` à marquer la couche d'accès aux données, et `@Component` à déclarer un composant Spring plus générique.  
Dans une application moderne, l'injection par constructeur est généralement préférable à l'injection de champ, car elle rend les dépendances plus explicites et améliore la testabilité.

## Comment une requête circule

Dans une API Spring Boot classique, une requête HTTP arrive dans un contrôleur, le contrôleur appelle un service, puis le service peut s'appuyer sur un repository pour lire ou écrire des données avant de renvoyer une réponse, souvent en JSON.  
Cette séparation en couches améliore la lisibilité, les tests et la maintenance du code.

## Convention over configuration

Spring Boot applique le principe de "convention over configuration" : il choisit des réglages raisonnables par défaut à partir des dépendances présentes dans le projet et de la configuration existante.  
Par exemple, si la dépendance web est présente, Spring Boot peut configurer automatiquement des éléments comme le serveur embarqué, le DispatcherServlet et l'infrastructure web nécessaire.  
Cette auto-configuration reste non intrusive, car il est possible de remplacer certains comportements en définissant explicitement ses propres beans ou sa propre configuration.

## Mini carte mentale

Pour mémoriser rapidement Spring Boot, il est utile de penser l'application ainsi :

- `Application` : point d'entrée du projet avec `SpringApplication.run(...)`.
- `Controller` : reçoit la requête HTTP.
- `Service` : contient la logique métier.
- `Repository` : dialogue avec la base de données.
- `Entity` : objet métier persisté en base de données (mappé à une table SQL via `@Entity`).
- `Bean` : objet géré par Spring dans le conteneur IoC.

Exemple mental simple : un contrôleur `UserController` reçoit `GET /users`, appelle `UserService`, qui lui-même peut appeler `UserRepository`, puis Spring renvoie la réponse au format JSON.

## Ce qu'il faut retenir aujourd'hui

Pour une première journée d'apprentissage, le plus utile est de comprendre dans cet ordre : `@SpringBootApplication`, les contrôleurs REST, les services, les beans, l'injection de dépendances, puis la configuration via `application.properties` ou `application.yml`.  
Il faut aussi retenir que les starters Spring Boot servent à ajouter rapidement des blocs techniques cohérents, comme le web ou l'accès aux données, sans assembler manuellement toutes les bibliothèques nécessaires.  
Si les notions de conteneur Spring, bean, injection de dépendances, autoconfiguration et architecture en couches sont claires, alors le socle essentiel de Spring Boot est déjà en place.

## Erreurs fréquentes au début

### Whitelabel Error Page / 404
Cette erreur apparaît quand aucune route ne correspond à l’URL demandée.  
Dans mon cas, cela voulait dire que `/` n’était pas encore défini.

### Port 8080 déjà utilisé
Si Spring Boot affiche `Adresse déjà utilisée`, cela veut dire qu’un autre programme occupe déjà le port 8080.  
Il faut alors arrêter l’ancien processus ou changer temporairement le port.

### À retenir
Une erreur au démarrage ne veut pas toujours dire que le code est faux.  
Elle peut aussi venir de l’environnement, du port réseau ou d’un composant manquant.

## Tester son API (1.3 - Test de l'endpoint)

Pour valider que l'API fonctionne, on ne se contente pas seulement du navigateur. Le navigateur effectue uniquement des requêtes `GET`. Pour la suite du développement de l'API (requêtes POST, PUT, DELETE, ajout de headers, d'authentification...), des outils spécialisés sont nécessaires.

### Outils disponibles :
- **Le navigateur** : Très basique, sert juste à s'assurer qu'une route GET répond (ex: `http://localhost:8080/heartbeat`).
- **Fichiers .http (IDE)** : Permettent de lancer rapidement des requêtes textuelles depuis IntelliJ ou VS Code (via l'extension REST Client).
- **Postman ou Bruno** : Ce sont de véritables clients API professionnels. **Bruno** est une alternative moderne, très légère et open-source face à Postman.

### Ce qu'il faut faire :
Installer **Bruno** (ou Postman), créer une nouvelle requête HTTP de type `GET` vers l'URL `http://localhost:8080/heartbeat`, et l'exécuter. Cela valide le dernier livrable de votre itération 1 !