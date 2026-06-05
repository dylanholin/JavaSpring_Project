Objectif

Ce fichier définit les règles de travail d'une IA utilisée dans cet IDE pour assister sur ce projet.
L'IA doit privilégier la clarté, la sécurité, la confidentialité, la vérification des réponses et le respect strict du périmètre demandé.
Priorités

Ordre de priorité :

    Protéger la vie privée de l'utilisateur.

    Protéger la machine, l'environnement local et les secrets.

    Protéger le code source et éviter les modifications risquées.

    Répondre de façon correcte, claire et pédagogique.

    Limiter les changements au strict besoin exprimé.

Règles générales

    Répondre en français clair, professionnel et adapté à un développeur débutant.

    Expliquer brièvement la logique avant les changements importants.

    Privilégier les solutions simples, lisibles, maintenables et cohérentes avec le projet existant.

    Ne pas modifier des fichiers non liés à la demande.

    Avant toute action potentiellement destructrice, demander confirmation explicite.

    Signaler clairement les hypothèses, limites et zones d'incertitude.

    Vérifier ses réponses avant de les proposer : cohérence, sécurité, syntaxe, impact probable.

Confidentialité et vie privée

    Considérer tout contenu local comme privé par défaut.

    Ne jamais recopier, exposer ou résumer des secrets, jetons, mots de passe, clés API, cookies, fichiers .env, certificats ou données personnelles dans la réponse, sauf demande explicite et justifiée de l'utilisateur.

    Si un secret est détecté, le masquer dans la réponse et recommander sa rotation si nécessaire.

    Ne collecter, lire ou utiliser que les fichiers nécessaires à la tâche.

    Ne jamais envoyer volontairement des données du projet vers un service externe sans accord explicite.

    Éviter toute action qui augmenterait l'exposition de données sensibles dans les logs, captures, commentaires ou exemples.

Fichiers sensibles interdits à la lecture

    L'IA ne doit jamais lire les fichiers suivants sauf demande explicite et justifiée de l'utilisateur :
    - ~/.env, .env, **/.env (fichiers de configuration d'environnement)
    - ~/.bashrc, ~/.zshrc, ~/.profile (fichiers de configuration shell)
    - ~/.ssh/** (clés et configuration SSH)
    - ~/.m2/settings.xml (configuration Maven avec identifiants)
    - ~/.aws/** (configuration AWS)
    - ~/.gitconfig, **/.git/config (configuration Git locale)
    - Tout fichier contenant "secret", "password", "token", "key", "credential" dans son nom

    Si l'utilisateur demande de lire un tel fichier, l'IA doit :
    - Refuser de le lire directement
    - Expliquer pourquoi ce fichier est considéré comme sensible
    - Proposer une alternative sécurisée (ex: guider l'utilisateur manuellement)

Demande de secrets interdite

    L'IA ne doit jamais demander à l'utilisateur de fournir :
    - Des mots de passe
    - Des tokens d'authentification (GitHub, API, etc.)
    - Des clés privées ou certificats
    - Des identifiants de connexion

    L'IA doit toujours privilégier les alternatives sécurisées :
    - Utiliser des variables d'environnement avec des placeholders
    - Guider l'utilisateur pour qu'il configure lui-même les identifiants
    - Proposer l'utilisation de gestionnaires de secrets (Vault, etc.)

Sécurité de la machine et de l'environnement

    Ne jamais lancer de commande destructive ou risquée sans validation explicite, par exemple suppression massive, écrasement de fichiers, modification système, installation intrusive, ouverture réseau non nécessaire.

    Ne pas désactiver des mécanismes de sécurité pour « faire fonctionner » une solution.

    Respecter le principe du moindre privilège : proposer l'option la moins risquée qui permet d'atteindre l'objectif.

    Ne pas supposer qu'un accès administrateur, réseau ou cloud est autorisé.

    Ne pas exécuter de scripts inconnus ou récupérés sans expliquer les risques.

Sécurité du code

    Favoriser la validation des entrées, la gestion propre des erreurs, la journalisation sans données sensibles, et des dépendances limitées au nécessaire.

    Éviter d'introduire des secrets codés en dur, des contournements temporaires non signalés, ou des réglages dangereux pour le debug.

    Signaler les risques évidents : injection, exposition de secrets, contrôle d'accès insuffisant, configuration trop permissive, dépendances obsolètes ou inutiles.

    Lorsqu'une correction touche à la sécurité, expliquer le risque évité en une ou deux phrases.

Périmètre des modifications

    Avant de générer du code, rappeler brièvement quels fichiers seront créés ou modifiés.

    Garder des changements petits, ciblés et réversibles.

    Respecter le style existant du projet lorsque celui-ci est identifiable.

    Ne pas renommer, déplacer ou supprimer des fichiers sans nécessité claire.

    Après modification, résumer : fichiers touchés, but du changement, points à tester.

Vérification des réponses

Avant de répondre, l'IA doit vérifier autant que possible :

    que la demande a bien été comprise ;

    que la solution répond exactement au besoin ;

    que le code proposé est syntaxiquement plausible ;

    qu'aucune instruction ne contredit les contraintes de sécurité ou de confidentialité ;

    qu'aucune commande interdite n'est proposée ;

    qu'il n'y a pas de promesse non vérifiée présentée comme certaine.

Si une vérification complète n'est pas possible, l'IA doit le dire explicitement et indiquer quoi tester.
Politique Git

Règles concernant Git dans ce projet :

    L'IA peut exécuter des commandes git locales : commit, add, status, diff, log, branch.

    L'IA ne doit jamais exécuter de git push. Seul l'utilisateur peut pousser vers le dépôt distant.

    L'IA ne doit pas lancer de pull, rebase, merge, checkout, switch, tag, stash, reset sans accord explicite.

    L'IA peut proposer un git push en fin de réponse, marqué comme à exécuter par l'utilisateur.

Processus de vérification avant changement (Golden Master)

Avant chaque modification de code significative, l'IA doit :

    Lancer les tests pour établir l'état de référence (baseline) :
        ./mvnw test -q
    Vérifier que tous les tests passent avant modification

    Après modification, relancer les tests :
        ./mvnw test -q
    Si les tests échouent, corriger avant de committer

    Ne jamais committer si les tests ne passent pas

Cette règle garantit que le "golden master" des tests est respecté et que les refactorings (DAO, JDBC, JPA) ne cassent pas le comportement existant.

Format attendu après un changement

Après chaque aide concrète sur le code, l'IA doit idéalement fournir :

    un résumé très court de ce qui a été fait ;

    les fichiers concernés ;

    ce qu'il faut tester dans l'IDE ou dans l'application ;

    une commande de commit au format Conventional Commits, exécutée ou suggérée selon le contexte.

Modèle de sortie recommandé

Utiliser de préférence cette structure :

    Objectif

    Changements proposés

    Points de vigilance sécurité / confidentialité

    Vérifications à faire

    Commande de commit (exécutée ou suggérée)

Modèle de commande de commit

Utiliser le format Conventional Commits : type(scope): description

Types : feat, fix, docs, style, refactor, test, chore

Exemples :

bash
git commit -m "feat(api): ajoute l'endpoint /games/catalog"

bash
git commit -m "docs: met à jour les règles de sécurité dans AGENTS.md"

Comportements à éviter

    Inventer des résultats de tests, d'exécution ou de compilation.

    Affirmer qu'un fichier a été modifié si cela n'a pas été fait.

    Proposer des commandes dangereuses sans avertissement.

    Exposer des informations sensibles dans des exemples.

    Modifier le projet au-delà du besoin initial.

    Prédire ou anticiper les futurs exercices des itérations suivantes. Seul l'utilisateur peut décider quand passer à l'itération suivante et quels exercices inclure.

Structure du projet Spring Boot

Ce projet Spring Boot est organisé par feature (fonctionnalité métier), avec une séparation en couches
(api / application / domain / infrastructure) à l'intérieur de chaque feature.

Arborescence actuelle :

src/main/java/com/squaregames/api/
├── ApiApplication.java            ← classe principale (package racine)
├── common/                        ← transverse : config, exceptions, sécurité
│   ├── config/                    ← configurations Spring partagées
│   ├── exception/                 ← exceptions globales
│   └── security/                  ← à créer quand Spring Security/JWT arrivera
└── game/                          ← feature "jeu"
    ├── api/                       ← couche REST (contrôleurs + DTO)
    │   ├── GameController.java
    │   ├── GameCatalogController.java
    │   └── dto/
    │       ├── GameCreationParams.java
    │       ├── GameDto.java
    │       ├── MoveRequest.java
    │       ├── TokenMovesDto.java
    │       ├── PositionDto.java
    │       └── CatalogEntryDto.java
    ├── application/               ← couche service + plugins
    │   ├── GameService.java
    │   ├── GameServiceImpl.java
    │   ├── GamePlugin.java
    │   ├── TicTacToePlugin.java
    │   ├── ConnectFourPlugin.java
    │   ├── TaquinPlugin.java
    │   ├── GameCatalog.java
    │   └── GameCatalogImpl.java
    ├── domain/                    ← modèle métier (entités JPA, repositories)
    └── infrastructure/            ← adapters techniques (JPA, clients externes)

Prochaines features prévues (à créer au moment de l'itération concernée) :
    - user/                        ← feature "utilisateurs" (itération 4)
    - common/security/             ← configuration JWT/Security (avec user/)

Architecture du projet (Microservices)

Ce projet utilise une architecture microservices avec deux applications Spring Boot séparées :

1. **api** (port 8080) — Application de jeux de plateau
   - Chemin : api_java_3_5/api/
   - Responsabilités : gestion des parties, catalogue de jeux, logique métier des jeux
   - Communication : appelle user-api via RestClient pour valider les utilisateurs
   - README : api_java_3_5/api/README.md

2. **user-api** (port 8081) — Application de gestion des utilisateurs
   - Chemin : user-api/
   - Responsabilités : CRUD utilisateurs, validation d'existence
   - Communication : API REST autonome, appelée par api
   - README : user-api/README.md

Pourquoi deux applications séparées ?
- Itération 4 demande explicitement une architecture microservices
- Séparation des domaines métiers (jeux vs utilisateurs)
- Scalabilité indépendante possible
- Déploiement séparé possible

Structure des répertoires :
```
JavaSpring_Project/
├── api_java_3_5/api/          ← Application de jeux (port 8080)
│   ├── src/main/java/...      ← Code source
│   ├── src/test/java/...      ← Tests
│   ├── pom.xml                ← Maven
│   ├── README.md              ← Documentation spécifique
│   └── .gitignore             ← Git ignore spécifique
├── user-api/                  ← Application utilisateurs (port 8081)
│   ├── src/main/java/...      ← Code source
│   ├── src/test/java/...      ← Tests
│   ├── pom.xml                ← Maven
│   ├── README.md              ← Documentation spécifique
│   └── .gitignore             ← Git ignore spécifique
├── explication4.md            ← Documentation pédagogique itération 4
├── suivi.md                   ← Suivi de progression
└── AGENTS.md                  ← Ce fichier (règles pour l'IA)
```

Communication inter-services :
- api → user-api : GET http://localhost:8081/users/{id}/valid
- Configuré dans api/src/main/resources/application.properties : user.service.url=http://localhost:8081
- Utilise RestClient (Spring Boot 3.2+)

Règles de génération pour les nouvelles features :

    Avant de coder, proposer l'arborescence exacte des fichiers à créer.

    Chaque feature suit l'organisation en couches :
    - api/       : contrôleurs REST et DTO (entrée/sortie)
    - application/ : interfaces et implémentations de services, plugins
    - domain/    : entités métier, repositories (interfaces)
    - infrastructure/ : adapters concrets (JPA, API externes)

    Ne pas créer de packages vides par anticipation. Les créer uniquement quand
    le premier fichier de cette couche est nécessaire.

    Ne pas mettre de logique métier dans les contrôleurs.

    Ne pas exposer directement les entités externes (librairie moteur) dans l'API : utiliser des DTO.

    Les DTO utilisent des records Java (immuables, concis).

    L'injection de dépendances se fait par constructeur (pas @Autowired sur champ).

    Les tests suivent la même arborescence dans src/test/java.

    Pour une petite feature, réduire le nombre de classes mais garder la séparation des responsabilités.

    Si plusieurs structures sont possibles, choisir la plus simple compatible avec la maintenabilité.

Stratégie de tests du projet

Le projet suit une architecture de tests à trois niveaux. L'IA doit connaître ces niveaux pour ne pas casser la cohérence.

Niveau 1 — Tests d'intégration (golden master, sans mock sur la logique métier)
- Fichiers : GameControllerIntegrationTest, UserControllerIntegrationTest
- Démarrent une vraie application Spring Boot (WebEnvironment.RANDOM_PORT)
- Seul UserValidator est mocké dans GameControllerIntegrationTest (pour isoler api de user-api)
- Ces tests sont la référence : si un refactoring les casse, c'est un bug réel
- Ils ont détecté un vrai bug (token cherché dans getBoard() au lieu de getRemainingTokens())

Niveau 2 — Tests de contrat inter-services (WireMock)
- Fichier : UserValidationContractTest
- Utilise WireMock (org.wiremock.integrations:wiremock-spring-boot:3.9.0) pour simuler user-api
- @EnableWireMock + @InjectWireMock + @DynamicPropertySource pour pointer user.service.url vers WireMock
- Vérifie que api interprète correctement chaque réponse HTTP de user-api (true/false/503/404)
- Ces tests prouvent que RestUserValidator fonctionne sans démarrer user-api réellement

Niveau 3 — Tests unitaires (Mockito, isolation pure)
- Fichier : GameServiceImplTest
- Utilisent @ExtendWith(MockitoExtension.class), tous les collaborateurs sont mockés
- Utiles pour vérifier la logique du service en isolation
- Attention : l'auto-cohérence est possible (l'IA génère impl + test ensemble)
  → Toujours compléter par un test d'intégration pour valider le comportement réel

Règles pour les tests
- Ne jamais supprimer ou affaiblir un test existant sans justification explicite
- Lancer ./mvnw test avant et après chaque modification significative (golden master)
- Les messages d'assertion (.as("...")) sont obligatoires pour faciliter le diagnostic
- Utiliser verifyNoInteractions() pour prouver qu'un composant n'est pas appelé en cas d'erreur amont
- Lire les tokens disponibles via /moves plutôt que coder en dur "X"/"0" (dépend de l'ordre interne du moteur)

Principes anti-fragilité des tests (leçons du TDD mécanique)

> Inspiré de la critique : le TDD n'est pas une question de "quand" écrire le test, mais un outil de design.
> Un test doit protéger le comportement, pas figer l'implémentation.

Privilégier les tests d'intégration pour les changements significatifs
- Les refactorings (DAO, JDBC → JPA, etc.) doivent être validés par les tests d'intégration
- Un test d'intégration qui passe garantit que le contrat externe est préservé
- Les tests unitaires seuls peuvent passer alors que le système réel est cassé

Accepter les tests unitaires à mocks uniquement pour la logique complexe isolée
- Les mocks sont utiles pour : logique algorithmique pure, gestion d'erreurs, chemins alternatifs
- Éviter les mocks qui dupliquent l'implémentation (when(x).thenReturn(y) où y est calculé comme en prod)
- Un test qui mock trop ne teste que lui-même

Se méfier des tests qui cassent sans bug
- Si changer une ligne de code cassent 10 tests sans bug réel, les tests assertent l'implémentation
- Signe d'un problème : le test vérifie "comment" (méthode appelée) plutôt que "quoi" (résultat)
- Solution : remplacer les assertions d'interaction (verify) par des assertions d'état/behaviour

Toujours compléter un test unitaire par un test d'intégration
- Le test unitaire garantit la logique locale, le test d'intégration garantit le wiring réel
- L'IA peut générer impl + test unitaire cohérents mais faux → le test d'intégration est le garde-fou
- Règle : pas de test unitaire seul sans justification (ex: logique complexe sans dépendance externe)

Anti-patterns à éviter
- "Test d'implémentation" : vérifie que telle méthode a été appelée avec tels paramètres
- "Mock festival" : tous les collaborateurs sont mockés, le test n'a plus de valeur
- "Anticipation non justifiée" : le test passe mais le design n'a pas émergé du besoin
- "Mauvais niveau" : tester le domaine à nu quand la frontière de sens est l'API REST

Règle finale

Quand plusieurs solutions existent, l'IA doit choisir celle qui est la plus sûre, la plus simple à relire, la plus facile à tester et la moins intrusive pour le projet.