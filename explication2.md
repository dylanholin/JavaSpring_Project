# Itération 2 — API Jeu de plateau : ce qu'il faut comprendre

L'itération 2 fait passer votre application Spring Boot d'un simple endpoint `/heartbeat` à une **véritable API REST** capable de gérer des parties de jeux de plateau (morpion, puissance 4, taquin…).  
La logique de jeu est fournie par une **librairie externe** distribuée via GitHub Packages — vous vous concentrez sur l'architecture, pas sur les règles du jeu.

> 📁 **Légende :** `✅` = déjà créé | `📝` = à créer | Racine Java : `api_java_3_5/api/src/main/java/com/squaregames/api/`

---

## 1. Ajouter une dépendance Maven privée (GitHub Packages)

### Pourquoi c'est nouveau

Jusqu'ici, toutes vos dépendances venaient de Maven Central (public). Le moteur de jeu `engine` est hébergé sur un **dépôt GitHub privé** : Maven doit être configuré pour s'y authentifier.

### Ce qu'il faut faire

**Dans `pom.xml`** — deux choses à ajouter :

> 📁 `api_java_3_5/api/pom.xml` ✅

1. La `<dependency>` vers le moteur :
```xml
<dependency>
  <groupId>fr.le-campus-numerique.square-games</groupId>
  <artifactId>engine</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

2. Le `<repository>` qui pointe vers GitHub Packages :
```xml
<repository>
    <id>github</id>
    <name>GitHub le-campus-numerique Apache Maven Packages</name>
    <url>https://maven.pkg.github.com/le-campus-numerique/cda-java-spring-game-engine</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

**Dans `~/.m2/settings.xml`** — vos identifiants GitHub :

> 📁 `~/.m2/settings.xml` 📝 — hors projet, à créer dans votre répertoire utilisateur Linux

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_USERNAME}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

> ⚠️ **Sécurité** : ne codez jamais votre token GitHub en dur dans `settings.xml`. Utilisez des variables d'environnement (`GITHUB_USERNAME`, `GITHUB_TOKEN`) ou à défaut, assurez-vous que le fichier `~/.m2/settings.xml` n'est jamais commité.

### La logique derrière

- L'`<id>github</id>` dans `pom.xml` (repository) doit correspondre à l'`<id>github</id>` dans `settings.xml` (server). C'est ce qui fait le lien entre « où trouver le paquet » et « comment s'authentifier ».
- Un **SNAPSHOT** est une version en cours de développement. Maven peut la re-télécharger régulièrement, contrairement à une version « release » figée.

### Syntaxe Maven décryptée

**`<dependency>` — une brique logicielle**
- `<groupId>` : identifiant de l'organisation ou du projet (ex: `fr.le-campus-numerique.square-games`).
- `<artifactId>` : nom du module (ex: `engine`).
- `<version>` : numéro de version. `1.0-SNAPSHOT` signifie "version 1.0 en cours de développement".
- Maven utilise ce triplet (groupId, artifactId, version) pour identifier de manière unique chaque bibliothèque — on appelle ça les **coordonnées Maven**.

**`<repository>` — où trouver la dépendance**
- Par défaut, Maven cherche sur Maven Central (public). Ici on ajoute un dépôt supplémentaire.
- `<id>` : nom interne utilisé par Maven pour faire le lien avec les identifiants du `settings.xml`.
- `<url>` : l'adresse du dépôt GitHub Packages.
- `<snapshots><enabled>true</enabled>` : autorise le téléchargement des versions SNAPSHOT depuis ce dépôt.

**`${env.GITHUB_USERNAME}` — une variable d'environnement**
- La syntaxe `${env.NOM_VARIABLE}` lit la variable d'environnement au moment de l'exécution de Maven.
- Cela évite de coder son token en dur dans le fichier. Il faut définir ces variables dans le terminal avant de lancer Maven :
  ```bash
  export GITHUB_USERNAME=ton_pseudo
  export GITHUB_TOKEN=ton_token
  ```

### Implémentation réalisée (étape 2.1)

**Fichiers modifiés :**
- `api_java_3_5/api/pom.xml` — ajout de la dépendance `engine`, du `<repository>` GitHub Packages, de Lombok et de Spring Boot DevTools

**Dépendances utilitaires ajoutées :**

| Dépendance | Rôle | Scope |
|---|---|---|
| **Lombok** | Génère automatiquement getters, setters, constructeurs via annotations (`@Getter`, `@Data`…). Réduit le code répétitif dans les DTOs. | `optional` (pas inclus dans le livrable final) |
| **Spring Boot DevTools** | Redémarre automatiquement l'application à chaque sauvegarde. Active LiveReload dans le navigateur. | `runtime` + `optional` (désactivé en production) |

> `<optional>true</optional>` signifie que cette dépendance ne se propage pas aux projets qui dépendraient du vôtre. Elle reste locale au développement.

**Fichiers créés :**
- `api/.../game/application/GameCatalog.java` — interface avec `getGameIdentifiers()`
- `api/.../game/application/GameCatalogImpl.java` — implémentation s'appuyant sur une instance de `TicTacToeGameFactory`
- `api/.../game/api/GameCatalogController.java` — endpoint `GET /games/catalog`

**Factories disponibles dans le moteur :**

| Factory | ID | Joueurs | Plateau |
|---|---|---|---|
| `TicTacToeGameFactory` | `tictactoe` | 2 | 3 à 5 |
| `ConnectFourGameFactory` | `connect4` | 2 | 7 |
| `TaquinGameFactory` | `15 puzzle` | 1 | 3 à 8 |

**Prérequis avant compilation :** configurer `~/.m2/settings.xml` avec les variables d'environnement `GITHUB_USERNAME` et `GITHUB_TOKEN`, puis re-importer le projet Maven dans l'IDE.

### Implémentation réalisée (étape 2.3)

> 📁 Racine : `api_java_3_5/api/src/main/java/com/squaregames/api/game/` (répartis en `api/`, `api/dto/`, `application/`)

**Fichiers créés :**
- `api/dto/GameCreationParams.java` ✅ — DTO d'entrée (reçoit le JSON du client)
- `api/dto/GameDto.java` ✅ — DTO de sortie (envoyé au client en JSON)
- `api/dto/MoveRequest.java` ✅ — DTO pour jouer un coup (tokenName, row, col)
- `api/dto/TokenMovesDto.java` ✅ — DTO listant les coups possibles d'un token
- `api/dto/PositionDto.java` ✅ — DTO représentant une position (row, col)
- `application/GameService.java` ✅ — interface du service (contrat)
- `application/GameServiceImpl.java` ✅ — implémentation avec stockage en mémoire (`HashMap`)
- `api/GameController.java` ✅ — contrôleur REST exposant 5 endpoints

**Note :** `GameFactoryConfig.java` a été supprimé en 2.4 — les factories sont maintenant gérées directement par les plugins.

**Endpoints disponibles :**

| Verbe | URL | Description |
|---|---|---|
| `POST` | `/games` | Créer une nouvelle partie |
| `GET` | `/games` | Lister toutes les parties en cours |
| `GET` | `/games/{gameId}` | Voir une partie par son ID |
| `GET` | `/games/{gameId}/moves` | Voir les coups possibles |
| `POST` | `/games/{gameId}/moves` | Jouer un coup |

**Exemple de requête POST `/games` :**
```json
{
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3
}
```

**Exemple de réponse `GameDto` :**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3,
  "status": "ONGOING"
}
```

**Points clés de `GameServiceImpl` :**
- `Map<UUID, Game> games` : stockage en mémoire (les parties sont perdues au redémarrage — base de données à l'itération 3).
- `Collection<GameFactory> gameFactories` : remplacé par `List<GamePlugin> plugins` en 2.4. Spring injecte automatiquement tous les `@Component` qui implémentent `GamePlugin`.
- `getPossibleMoves()` : parcourt les tokens sur le plateau et en attente, retourne leurs `getAllowedMoves()`.
- `playMove()` : trouve le token par son nom, appelle `moveTo()` avec la position cible. L'exception `InvalidPositionException` est convertie en HTTP 400.
- `findGame()` : méthode utilitaire privée qui factorise la recherche d'une partie + HTTP 404 si absente.
- `toDto(Game game)` : méthode privée qui convertit un objet `Game` (domaine) en `GameDto` (transport).

**Pourquoi `GameFactoryConfig` a été supprimé :**
- Les plugins (`TicTacToePlugin`, etc.) sont annotés `@Component` → Spring les détecte automatiquement.
- Chaque plugin possède sa propre factory en champ privé — pas besoin de beans séparés.
- `GameServiceImpl` injecte maintenant `List<GamePlugin>` au lieu de `Collection<GameFactory>`.

**Points clés de `GameController` :**
- `@RequestMapping("/games")` : préfixe commun à tous les endpoints de ce contrôleur. Évite la répétition dans chaque `@GetMapping`/`@PostMapping`.
- `@PathVariable UUID gameId` : Spring convertit automatiquement la chaîne de l'URL en objet `UUID`.

### Implémentation réalisée (étape 2.4)

> 📁 Racine : `api_java_3_5/api/src/main/java/com/squaregames/api/game/` (répartis en `api/`, `api/dto/`, `application/`)

**Fichiers créés :**
- `application/GamePlugin.java` ✅ — interface avec `getFactory()`, `createGame()`, `getName(Locale)`, `getGameType()`
- `application/TicTacToePlugin.java` ✅ — plugin Morpion avec `@Value` et `MessageSource`
- `application/ConnectFourPlugin.java` ✅ — plugin Puissance 4 avec `@Value` et `MessageSource`
- `application/TaquinPlugin.java` ✅ — plugin Taquin avec `@Value` et `MessageSource`
- `api/dto/CatalogEntryDto.java` ✅ — DTO pour une entrée du catalogue (gameType + nom traduit)

**Fichiers de ressources créés :**
- `src/main/resources/application.properties` ✅ — valeurs par défaut (`game.tictactoe.default-player-count=2`, etc.)
- `src/main/resources/messages.properties` ✅ — noms en français (défaut)
- `src/main/resources/messages_en.properties` ✅ — noms en anglais

**Fichiers modifiés :**
- `GameServiceImpl.java` — injecte `List<GamePlugin>` au lieu de `Collection<GameFactory>`
- `GameCatalogController.java` — utilise `GamePlugin` + `LocaleContextHolder` pour les noms traduits
- `GameFactoryConfig.java` — supprimé (devenu inutile)

**Architecture finale :**
```
Requête HTTP → Controller → Service → Plugin → Moteur de jeu
                    ↑            ↑         ↑
                  DTO          DTO      @Value + MessageSource
```

**Points clés de `GamePlugin` :**
- `getFactory()` : expose la factory interne pour permettre la création avec paramètres spécifiques (POST /games).
- `createGame()` : crée une partie avec les valeurs par défaut (sans paramètres).
- `getName(Locale)` : retourne le nom traduit du jeu via `MessageSource`.
- `getGameType()` : retourne l'identifiant unique du jeu (doit correspondre au `gameType` envoyé par le client).

**Points clés des plugins :**
- `@Component` : enregistre le plugin comme bean Spring → automatiquement injecté dans `List<GamePlugin>`.
- `@Value("${...}")` : lit les valeurs par défaut depuis `application.properties`. Changez le fichier, pas le code.
- `MessageSource` : injecté par constructeur, utilisé dans `getName()` pour lire `messages.properties`.
- La factory est stockée comme champ `private final` — créée une seule fois.

**Points clés de `GameCatalogController` (v2) :**
- `List<GamePlugin> plugins` : Spring injecte automatiquement tous les plugins.
- `LocaleContextHolder.getLocale()` : récupère la locale de la requête HTTP courante (basée sur le header `Accept-Language`).
- Retourne une `List<CatalogEntryDto>` avec le `gameType` et le `name` traduit.

**Exemple de réponse `GET /games/catalog` (Accept-Language: fr) :**
```json
[
  {"gameType": "tictactoe", "name": "Morpion"},
  {"gameType": "connect4", "name": "Puissance 4"},
  {"gameType": "15 puzzle", "name": "Taquin"}
]
```

**Exemple de réponse `GET /games/catalog` (Accept-Language: en) :**
```json
[
  {"gameType": "tictactoe", "name": "Tic Tac Toe"},
  {"gameType": "connect4", "name": "Connect Four"},
  {"gameType": "15 puzzle", "name": "Fifteen Puzzle"}
]
```

---

## 2. Concevoir une API REST (avant de coder)

### Les principes REST à retenir

| Action | Verbe HTTP | URL exemple |
|---|---|---|
| Créer une partie | `POST` | `/games` |
| Lister les parties | `GET` | `/games` |
| Voir une partie | `GET` | `/games/{gameId}` |
| Jouer un coup | `POST` | `/games/{gameId}/moves` |
| Voir les coups possibles | `GET` | `/games/{gameId}/moves` |

- **POST** = création ou action (corps de requête JSON).
- **GET** = lecture (pas de corps, paramètres dans l'URL).
- Les ressources sont nommées au pluriel (`/games`, pas `/game`).
- L'identifiant est dans le chemin (`/games/123`), pas en paramètre (`/games?id=123`).

### Syntaxe des URLs REST

**`{gameId}` — une variable de chemin (path variable)**
- Les accolades `{}` dans une URL Spring représentent une partie variable.
- Exemple : `/games/abc-123` → `gameId` vaudra `"abc-123"`.
- En Java, on récupère cette valeur avec l'annotation `@PathVariable`.

**`/games/{gameId}/moves` — une sous-ressource**
- `moves` est une sous-ressource de `games/{gameId}`.
- Cela signifie "les coups de la partie numéro gameId".
- Cette hiérarchie dans l'URL reflète la relation logique entre les objets.

### Le modèle du moteur de jeu

Le moteur fournit ces concepts clés :

- **`Game`** : une partie en cours (interface).
- **`Token`** : un pion/jeton. Il peut être sur le plateau (`board`), en attente (`remainingTokens`) ou retiré (`removedTokens`).
- **`GameFactory#createGame(playerCount, boardSize)`** : crée une nouvelle partie.
- Les joueurs sont identifiés par un **UUID** généré automatiquement — il n'y a pas de classe `Player`.

---

## 3. Architecture Controller / Service / DTO

C'est le cœur de l'itération. Vous passez d'un contrôleur unique à une **architecture en couches**.

### Le rôle de chaque couche

```
Requête HTTP → Controller → Service → Moteur de jeu
                    ↑            ↑
                  DTO          DTO
```

- **Controller** (`@RestController`) : reçoit la requête HTTP, valide les entrées, appelle le service, retourne la réponse HTTP (JSON). Il ne contient **aucune logique métier**.
- **Service** (`@Service`) : contient la logique métier. Il manipule les objets du domaine (`Game`, `Token`) et orchestre les opérations.
- **DTO** (Data Transfer Object) : une classe simple (record ou POJO) qui sert uniquement à transporter des données entre le client et le serveur. Il **sépare le modèle de transport du modèle de domaine**.

### Exemple concret

> 📁 `.../game/api/dto/GameCreationParams.java` 📝 — DTO, à créer à l'étape 2.3

```java
// DTO pour la création d'une partie
public record GameCreationParams(
    String gameType,    // "tictactoe", "connectfour"...
    int playerCount,
    int boardSize
) {}

// Controller
@RestController
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/games")
    public GameDto createGame(@RequestBody GameCreationParams params) {
        return gameService.createGame(params);
    }

    @GetMapping("/games/{gameId}")
    public GameDto getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }
}
```

> 📁 `.../game/api/GameController.java` 📝 — Controller, à créer à l'étape 2.3

### Syntaxe Java et Spring décryptée

**`record` — une classe immuable en une ligne (Java 14+)**
- Un `record` génère automatiquement : constructeur, `equals()`, `hashCode()`, `toString()`, et des getters (sans le préfixe `get`, ex: `params.gameType()`).
- Idéal pour les DTO car ce sont des objets simples qui ne font que transporter des données, sans logique.
- Équivalent ancien (POJO) : une classe avec champs `private final`, constructeur, et getters — beaucoup plus verbeux.

**`@RestController` — cette classe répond à des requêtes HTTP**
- Combine `@Controller` + `@ResponseBody` : chaque méthode retourne directement du JSON dans la réponse HTTP.
- Spring crée automatiquement une instance de cette classe (bean) grâce au scan des composants.

**`@PostMapping("/games")` — écoute les requêtes POST sur `/games`**
- Le verbe HTTP est dans le nom de l'annotation : `@PostMapping` = POST, `@GetMapping` = GET, `@PutMapping` = PUT, `@DeleteMapping` = DELETE.
- Le chemin entre parenthèses est relatif à la racine du serveur.

**`@RequestBody GameCreationParams params` — convertir le JSON en objet Java**
- Spring lit le corps de la requête HTTP (du JSON), et le convertit automatiquement en instance de `GameCreationParams`.
- Cette conversion s'appelle la **désérialisation** — elle est faite par Jackson, inclus dans `spring-boot-starter-web`.
- Si le JSON est mal formé ou ne correspond pas aux champs du record, Spring renvoie une erreur 400.

**`@PathVariable UUID gameId` — Spring convertit automatiquement le type**
- Lie la variable `{gameId}` de l'URL au paramètre Java `gameId`.
- Le nom du paramètre Java doit correspondre au nom entre accolades dans l'URL.
- Spring est capable de convertir automatiquement une chaîne en `UUID`, `int`, `long`, etc. Si la conversion échoue (ex: "abc" vers `UUID`), Spring retourne une erreur 400.

**`private final GameService gameService` — un champ immuable**
- `final` garantit que la référence ne peut pas être modifiée après la construction. C'est une bonne pratique pour les dépendances injectées.

**Injection par constructeur — la méthode recommandée**
- Le constructeur `public GameController(GameService gameService)` reçoit le service en paramètre.
- Spring appelle automatiquement ce constructeur en fournissant le bean `GameService`.
- Avantages par rapport à `@Autowired` sur un champ : dépendances explicites, testabilité (on peut passer un mock), champ `final` possible.

### Pourquoi les DTO sont importants

- Si vous exposez directement l'objet `Game` du moteur, vous couplez votre API à la librairie. Le jour où la librairie change, votre API casse.
- Les DTO vous permettent de **choisir exactement ce que vous exposez** (masquer des champs internes, renommer des propriétés, ajouter des informations calculées).

### DTO ≠ Entity

Ne confonds pas **DTO** (Data Transfer Object) et **Entity** (entité JPA) :

| | DTO | Entity |
|---|---|---|
| Rôle | Transporter des données (client ↔ serveur) | Représenter un objet métier persistant |
| Annotation | Aucune (simple `record` Java) | `@Entity` (JPA) |
| Lien base de données | Aucun | Mappé à une table SQL |
| Présent en itération 2 | ✅ Oui | ❌ Non (arrive en itération 3) |

> 📌 En itération 2, les données sont stockées en mémoire (`HashMap`). L'itération 3 introduira JPA, les `@Entity` et les `Repository` pour persister les données en base.

### Syntaxe avancée introduite dans GameServiceImpl

**`Stream.concat(stream1, stream2)` — fusionner deux flux**
- Combine deux `Stream` en un seul. Ici, on fusionne les tokens sur le plateau (`game.getBoard().values()`) et les tokens en attente (`game.getRemainingTokens()`).
- Cela permet de parcourir tous les tokens d'un jeu en une seule opération, quel que soit leur emplacement.

**`CellPosition` — une position sur le plateau**
- Classe fournie par le moteur. Elle contient deux champs : `x()` (colonne) et `y()` (ligne).
- `new CellPosition(row, col)` crée une position. Attention : `x` = colonne, `y` = ligne.
- `token.getPosition()` retourne `null` si le token n'est pas sur le plateau (token en attente).

**`InvalidPositionException` — coup invalide**
- Levée par `token.moveTo()` quand la position demandée n'est pas dans `getAllowedMoves()`.
- On la capture avec `try-catch` et on la convertit en `ResponseStatusException` pour renvoyer HTTP 400 au client.

**`ResponseStatusException` — erreur HTTP explicite**
- Exception Spring qui permet de retourner un code HTTP spécifique (400, 404…) avec un message.
- `new ResponseStatusException(HttpStatus.NOT_FOUND, "message")` → HTTP 404.
- `new ResponseStatusException(HttpStatus.BAD_REQUEST, "message")` → HTTP 400.
- Plus propre que `IllegalArgumentException` qui produit un HTTP 500 générique.

**`@Configuration` et `@Bean` — déclarer manuellement un bean Spring**
- `@Configuration` : indique que la classe contient des méthodes `@Bean`.
- `@Bean` sur une méthode : Spring appelle cette méthode au démarrage et enregistre l'objet retourné comme un bean.
- Utilisé dans `GameFactoryConfig` car les factories du moteur n'ont pas `@Component` (on ne peut pas modifier la librairie externe).
- Équivalent à écrire `@Component` sur la classe, mais sans toucher au code source de la librairie.

---

## 4. GameCatalog et GamePlugin : le pattern d'extension

### GameCatalog

Une interface simple qui liste les jeux disponibles :

> 📁 `.../game/application/GameCatalog.java` ✅

```java
public interface GameCatalog {
    Collection<String> getGameIdentifiers();
}
```

**Syntaxe :**
- `interface` : définit un **contrat** — toute classe qui l'implémente doit fournir les méthodes déclarées.
- `Collection<String>` : type de retour. `Collection` est l'interface parente de `List`, `Set`, etc. On utilise le type le plus générique possible pour laisser la liberté d'implémentation.
- `getGameIdentifiers()` : méthode sans paramètre qui retourne les identifiants des jeux.

Son implémentation (`GameCatalogImpl`) s'appuie sur les `GameFactory` du moteur. Un contrôleur expose cette liste via `GET /games/catalog`.

### GamePlugin

Le moteur est générique : il ne sait pas comment s'appelle un jeu en français, ni quels sont les paramètres par défaut.  
C'est le rôle du **plugin** : enrichir chaque type de jeu avec des **données de présentation**.

> 📁 `.../game/application/GamePlugin.java` ✅

```java
public interface GamePlugin {
    GameFactory getFactory();              // expose la factory pour création avec paramètres
    Game createGame();                    // crée avec les paramètres par défaut
    String getName(Locale locale);        // nom traduit du jeu
    String getGameType();                 // identifiant unique ("tictactoe", "connectfour"...)
}
```

**Syntaxe :**
- `Game createGame()` : retourne un objet de type `Game` (l'interface du moteur). La factory concrète (TicTacToe, ConnectFour…) est cachée derrière cette méthode.
- `String getName(Locale locale)` : prend une `Locale` en paramètre (ex: `Locale.FRENCH`, `Locale.ENGLISH`) et retourne le nom traduit. La `Locale` représente une langue/région.
- `String getGameType()` : retourne l'identifiant unique du type de jeu, qui doit correspondre au `getGameFactoryId()` de la factory.

Chaque jeu a son plugin : `TicTacToePlugin`, `ConnectFourPlugin`, `TaquinPlugin`…

### Injection automatique de tous les plugins

> 📁 `.../game/application/GameServiceImpl.java` ✅

```java
@Service
public class GameServiceImpl implements GameService {
    private final List<GamePlugin> plugins; // Spring injecte TOUS les plugins

    public GameServiceImpl(List<GamePlugin> plugins) {
        this.plugins = plugins;
    }
}
```

**Syntaxe :**
- `List<GamePlugin> plugins` : le type est `List<GamePlugin>` (une liste d'objets implémentant `GamePlugin`). Spring détecte qu'il faut injecter **tous** les beans de type `GamePlugin` présents dans le contexte.
- C'est une fonctionnalité puissante de Spring : si vous déclarez une `List<X>` ou `Map<String, X>` dans un constructeur, Spring collecte automatiquement tous les beans du type `X`.
- `implements GameService` : la classe respecte le contrat défini par l'interface `GameService`.

Spring détecte automatiquement toutes les classes `@Component` qui implémentent `GamePlugin` et les injecte dans la liste. Ajouter un nouveau jeu revient juste à créer un nouveau plugin — le service n'a pas besoin d'être modifié.

---

## 5. @Value et configuration externalisée

### Le problème

Jusqu'ici, vos valeurs étaient codées en dur dans le code Java. Pour le moteur de jeu, `playerCount` et `boardSize` sont des paramètres obligatoires… mais un morpion a **toujours** 2 joueurs et un plateau 3×3.

### La solution : `application.properties`

> 📁 `.../resources/application.properties` 📝 — à enrichir à l'étape 2.4

```properties
game.tictactoe.default-player-count=2
game.tictactoe.default-board-size=3
game.connectfour.default-player-count=2
game.connectfour.default-board-size=7
```

### L'injection avec @Value

> 📁 `.../game/application/TicTacToePlugin.java` 📝 — plugin, à créer à l'étape 2.4

```java
@Component
public class TicTacToePlugin implements GamePlugin {
    @Value("${game.tictactoe.default-player-count}")
    private int defaultPlayerCount;

    @Value("${game.tictactoe.default-board-size}")
    private int defaultBoardSize;

    @Override
    public Game createGame() {
        return new TicTacToeGameFactory()
            .createGame(defaultPlayerCount, defaultBoardSize);
    }
}
```

**Syntaxe :**
- `@Value("${...}")` : injecte la valeur d'une propriété dans le champ. Le chemin entre `${}` correspond à une clé dans `application.properties`.
- La conversion de type est automatique : la valeur `"2"` dans le fichier `.properties` est convertie en `int 2`.
- Si la propriété est absente, Spring lance une erreur au démarrage (sauf si on fournit une valeur par défaut avec `:` — ex: `${ma.cle:42}`).
- `@Component` : enregistre cette classe comme bean Spring. Sans cette annotation, `@Value` ne fonctionnerait pas car Spring ne gérerait pas l'instance.
- `@Override` : indique que la méthode redéfinit une méthode de l'interface parente. Le compilateur vérifie que la signature correspond bien — protection contre les fautes de frappe.

### Pourquoi c'est puissant

- Changer une valeur ne nécessite pas de recompiler le code.
- On peut surcharger ces valeurs par **profil** (`application-dev.properties`, `application-prod.properties`).
- On peut les passer en **variables d'environnement** au déploiement.

---

## 6. MessageSource et internationalisation (i18n)

### Le problème

Le nom d'un jeu doit s'afficher en français, en anglais, etc. selon la langue du client.

### La solution : `messages.properties`

Créez un fichier `src/main/resources/messages.properties` :

> 📁 `.../resources/messages.properties` 📝 — à créer à l'étape 2.4

```properties
game.tictactoe.name=Morpion
game.connectfour.name=Puissance 4
game.taquin.name=Taquin
```

Et un fichier `messages_en.properties` pour l'anglais :

> 📁 `.../resources/messages_en.properties` 📝 — à créer à l'étape 2.4

```properties
game.tictactoe.name=Tic Tac Toe
game.connectfour.name=Connect Four
game.taquin.name=Fifteen Puzzle
```

### L'utilisation dans un plugin

> 📁 `.../game/application/TicTacToePlugin.java` 📝 — même fichier que section 5, version enrichie avec MessageSource

```java
@Component
public class TicTacToePlugin implements GamePlugin {
    private final MessageSource messageSource;

    public TicTacToePlugin(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getName(Locale locale) {
        return messageSource.getMessage("game.tictactoe.name", null, locale);
    }
}
```

**Syntaxe :**
- `MessageSource` : interface Spring qui donne accès aux messages traduits. Spring Boot configure automatiquement un bean `MessageSource` qui lit les fichiers `messages*.properties`.
- `messageSource.getMessage(clé, args, locale)` :
  - `"game.tictactoe.name"` : la clé dans le fichier `.properties`.
  - `null` : arguments optionnels pour les messages paramétrés (ex: `"Bonjour {0}"`). Ici pas d'arguments.
  - `locale` : la langue demandée. Si le fichier pour cette locale n'existe pas, Spring utilise le fichier par défaut (`messages.properties`).

### Comment Spring choisit la langue

Le client envoie un header HTTP `Accept-Language: fr` ou `Accept-Language: en`.  
Dans votre contrôleur, vous pouvez récupérer la `Locale` avec `LocaleContextHolder.getLocale()`.

**`LocaleContextHolder`** est un holder Spring qui stocke la locale de la requête courante. C'est un ThreadLocal — chaque requête HTTP a sa propre locale, isolée des autres.

---

## 7. Syntaxe et annotations à connaître — récapitulatif

### Annotations Spring

| Annotation | Rôle | Exemple |
|---|---|---|
| `@RestController` | Expose une classe comme API REST (JSON) | `public class GameController` |
| `@Service` | Déclare une classe de logique métier | `public class GameServiceImpl` |
| `@Component` | Bean Spring générique | `public class TicTacToePlugin` |
| `@Configuration` | Classe contenant des définitions de beans | `public class GameFactoryConfig` |
| `@Bean` | Déclare un bean manuellement (méthode) | `@Bean public GameFactory ticTacToeGameFactory()` |
| `@Value("${...}")` | Injecte une propriété externe | `@Value("${game.tictactoe.default-player-count}")` |
| `@PostMapping("/url")` | Écoute les requêtes POST | `@PostMapping("/games")` |
| `@GetMapping("/url")` | Écoute les requêtes GET | `@GetMapping("/games/{id}")` |
| `@RequestBody` | Convertit le corps JSON en objet Java | `@RequestBody GameCreationParams p` |
| `@PathVariable` | Extrait une variable de l'URL | `@PathVariable String gameId` |
| `@Override` | Redéfinit une méthode d'interface/classe parente | `@Override public Game createGame()` |

### Syntaxe Java

| Élément | Description |
|---|---|
| `record` | Classe immuable simplifiée (Java 14+), génère constructeur + getters |
| `interface` | Définit un contrat que les classes doivent respecter |
| `implements` | Une classe respecte le contrat d'une interface |
| `private final` | Champ immuable, initialisé une fois dans le constructeur |
| `List<X>` | Liste typée — Spring injecte tous les beans de type X |
| `Collection<X>` | Interface parente de List/Set, plus générique |
| `Stream.concat(a, b)` | Fusionne deux flux en un seul |
| `CellPosition` | Position (x=colonne, y=ligne) fournie par le moteur |
| `Locale` | Représente une langue/région (ex: `Locale.FRENCH`) |
| `UUID` | Identifiant universel unique (ex: `550e8400-e29b-...`) |
| `try-catch` | Capture une exception pour la traiter (ex: `InvalidPositionException`) |

### Syntaxe Maven (pom.xml)

| Élément | Description |
|---|---|
| `<groupId>` | Organisation ou projet |
| `<artifactId>` | Nom du module |
| `<version>` | Numéro de version (`1.0-SNAPSHOT` = en développement) |
| `<repository>` | Dépôt où trouver les dépendances |
| `${env.VAR}` | Variable d'environnement dans settings.xml |

### Syntaxe HTTP

| Élément | Description |
|---|---|
| `POST` | Créer ou effectuer une action (avec corps JSON) |
| `GET` | Lire des données (pas de corps) |
| `{variable}` | Partie variable de l'URL (path variable) |
| `Accept-Language: fr` | Header HTTP indiquant la langue souhaitée |
| `Content-Type: application/json` | Header indiquant que le corps est du JSON |

---

## 8. Mini carte mentale de l'itération 2

```
api_java_3_5/api/
├── pom.xml ✅
│   ├── <dependency> engine (moteur de jeu)
│   └── <repository> GitHub Packages privé
│
├── games.http 📝
│
├── src/main/resources/
│   ├── application.properties 📝
│   ├── messages.properties 📝
│   └── messages_en.properties 📝
│
└── src/main/java/com/squaregames/api/
    ├── ApiApplication.java ✅
    ├── HeartbeatController.java ✅
    ├── HeartbeatSensor.java ✅
    ├── RandomHeartbeat.java ✅
    ├── common/
    │   ├── config/                    (prêt pour configs partagées)
    │   └── exception/                (prêt pour exceptions globales)
    └── game/
        ├── api/
        │   ├── GameController.java ✅
        │   ├── GameCatalogController.java ✅
        │   └── dto/
        │       ├── GameCreationParams.java ✅
        │       ├── GameDto.java ✅
        │       ├── MoveRequest.java ✅
        │       ├── TokenMovesDto.java ✅
        │       ├── PositionDto.java ✅
        │       └── CatalogEntryDto.java ✅
        ├── application/
        │   ├── GameCatalog.java ✅
        │   ├── GameCatalogImpl.java ✅
        │   ├── GameService.java ✅
        │   ├── GameServiceImpl.java ✅
        │   ├── GamePlugin.java ✅
        │   ├── TicTacToePlugin.java ✅
        │   ├── ConnectFourPlugin.java ✅
        │   └── TaquinPlugin.java ✅
        ├── domain/                    (prêt pour entités JPA)
        └── infrastructure/            (prêt pour adapters JPA)

~/.m2/settings.xml 📝 (hors projet)
└── <server> github → identifiants GitHub
```

---

## 8. Erreurs fréquentes de l'itération 2

### Échec de téléchargement de la dépendance engine

```
Could not resolve dependency fr.le-campus-numerique.square-games:engine:1.0-SNAPSHOT
```

**Causes possibles :**
- Le token GitHub est expiré ou n'a pas les droits `read:packages`.
- L'`<id>` du repository dans `pom.xml` ne correspond pas à l'`<id>` du server dans `settings.xml`.
- Le fichier `settings.xml` est mal placé (doit être dans `~/.m2/`, pas dans le projet).
- Le token est codé en dur mais mal copié (espaces, sauts de ligne).

**Vérification :** `mvn dependency:resolve -U` en ligne de commande pour voir l'erreur exacte.

### 404 sur un endpoint

Vérifiez :
- L'URL exacte (attention au pluriel : `/games` vs `/game`).
- L'annotation `@RequestMapping` ou `@GetMapping` sur la méthode.
- Que le contrôleur est bien scanné par Spring (dans un package sous `@SpringBootApplication`).

### 400 Bad Request sur POST

- Le JSON envoyé ne correspond pas aux champs du DTO.
- Les noms de champs sont sensibles à la casse (`gameType` ≠ `gametype`).
- Il manque `@RequestBody` sur le paramètre.

### NullPointerException dans un plugin

- Vous avez oublié `@Component` sur le plugin → Spring ne l'instancie pas.
- `@Value` ne fonctionne que dans un bean Spring (pas dans une classe instanciée avec `new`).
- La propriété dans `application.properties` est mal orthographiée ou absente.

### Le nom du jeu ne se traduit pas

- Le fichier `messages_en.properties` est mal nommé ou mal placé (doit être dans `src/main/resources/`).
- Vous n'avez pas configuré un `MessageSource` bean (Spring Boot en crée un par défaut, mais vérifiez).
- Le header `Accept-Language` n'est pas envoyé par le client.

---

## 9. Tester son API (au-delà du GET)

L'itération 2 introduit des requêtes `POST` avec corps JSON. Le navigateur ne suffit plus.

### Outils recommandés

- **Fichiers `.http`** dans l'IDE : rapide, versionné avec le code.
- **Bruno** ou **Postman** : client API complet pour tester tous les verbes, headers, etc.

### Exemple de fichier `.http` pour tester

> 📁 `api_java_3_5/api/games.http` 📝 — à créer pour tester l'API

```http
### Créer une partie de morpion
POST http://localhost:8080/games
Content-Type: application/json

{
  "gameType": "tictactoe",
  "playerCount": 2,
  "boardSize": 3
}

### Récupérer une partie
GET http://localhost:8080/games/{{gameId}}

### Voir le catalogue des jeux (en français)
GET http://localhost:8080/games/catalog
Accept-Language: fr
```

**Syntaxe du format `.http` :**
- `###` : début d'une requête (le texte après sert de commentaire/nom).
- `POST http://localhost:8080/games` : verbe HTTP + URL complète.
- `Content-Type: application/json` : header HTTP (un par ligne).
- Ligne vide après les headers, puis le corps JSON.
- `{{gameId}}` : variable — à remplacer manuellement ou définie ailleurs dans le fichier.
- Chaque requête est indépendante, on peut en exécuter une seule à la fois dans l'IDE.

---

## 10. Ce qu'il faut retenir

1. **Maven + GitHub Packages** : une dépendance privée nécessite un token dans `settings.xml` et un `<repository>` dans `pom.xml` avec le même `<id>`.
2. **REST** : `POST` pour créer/agir, `GET` pour lire, ressources au pluriel, identifiant dans l'URL.
3. **DTO** : ne jamais exposer directement les objets du domaine ou de la librairie externe.
4. **Architecture en couches** : Controller → Service → Plugin → Moteur. Chaque couche a une responsabilité unique.
5. **@Value** : externaliser les valeurs par défaut dans `application.properties`.
6. **MessageSource** : traduire les noms de jeux via `messages.properties` et le header `Accept-Language`.
7. **List<GamePlugin>** : Spring injecte automatiquement tous les plugins, ce qui rend l'ajout d'un nouveau jeu trivial.
