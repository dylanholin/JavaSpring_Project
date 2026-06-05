# Itération 6 — Journée IA : Remédiation & Méthodologie

> Ce document répond aux phases de la journée de remédiation par l'IA, basée sur les apprentissages du projet SquareGames (Spring Boot, microservices, JWT).

---

## Phase 1 — Bilan individuel des difficultés

### Difficulté 1 : Architecture hexagonale / Clean Architecture

**Contexte** : Organisation des packages par feature (`game/`, `user/`) avec sous-couches `api/`, `application/`, `domain/`, `infrastructure/`.

**Symptôme** : Difficulté à comprendre pourquoi l'interface `GameDao` est dans `application/` alors que son implémentation `JpaGameDao` est dans `infrastructure/`. Tentative de regrouper tout le DAO dans un package `dao/` unique (habitude MVC classique).

**Pourquoi c'est bloquant** : Sans cette compréhension, on crée des dépendances circulaires et on perd l'avantage de pouvoir swapper les implémentations (InMemory → JDBC → JPA sans toucher le service).

---

### Difficulté 2 : Persistance JPA avec reconstruction d'état

**Contexte** : Sauvegarde et restauration d'une partie de jeu en base de données.

**Symptôme** : La partie se sauvegarde, mais après redémarrage les tokens sont mal positionnés ou le plateau est vide. L'erreur ne génère pas d'exception — c'est un bug silencieux de logique.

**Tentatives** : Ajout de `gameDao.upsert(game)` dans `playMove`, mais la reconstruction via `GameFactory.createGame()` ne restaure pas l'état complet (tokens retirés, positions, joueurs).

**Solution finale** : Utilisation de `createGameWithIds()` avec récupération des tokens depuis la BDD et recréation de leur état (`isOnBoard`, `isRemoved`, positions).

---

### Difficulté 3 : Microservices et tests de contrat

**Contexte** : Communication entre `api` (port 8080) et `user-api` (port 8081) via `RestClient`.

**Symptôme** : Les tests d'intégration échouent si `user-api` n'est pas démarré. Comment tester `api` sans dépendance externe ?

**Découverte** : L'existence de WireMock pour simuler les réponses HTTP de `user-api` et valider le contrat inter-services sans démarrer le vrai service.

---

### Difficulté 4 : Spring Security + JWT stateless

**Contexte** : Authentification par token JWT, sans session serveur.

**Symptôme** : Erreurs 403 sur les endpoints protégés, malgré un token valide. Le filtre JWT n'était pas placé correctement dans la chaîne Spring Security.

**Tentatives** : Plusieurs configurations `addFilterBefore()` / `addFilterAfter()` avant de trouver la bonne place entre `UsernamePasswordAuthenticationFilter` et `ExceptionTranslationFilter`.

---

## Phase 2 — Remédiation par l'IA

### Cas 1 : Architecture hexagonale

**Requête IA** :
```
"Dans Spring Boot, explique-moi la différence entre organiser par couche technique 
(controller/service/dao) et organiser par feature avec des couches api/application/domain/infrastructure. 
Voici mon code actuel : [GameDao.java, JpaGameDao.java, GameServiceImpl.java]"
```

**Réponse IA** :
- L'IA a correctement expliqué le pattern Ports & Adapters
- Elle a identifié que `GameDao` est un "port" (interface définie par le métier)
- Elle a montré que `JpaGameDao` est un "adapter" (détail technique)

**Bilan** : ✅ Résolu — l'IA a été précise sur ce pattern architectural courant.

---

### Cas 2 : Requête JPQL pour `findByPlayerId`

**Requête IA** :
```
"J'ai une entité GameEntity avec un champ playerIds (String) qui contient des UUID 
séparés par des virgules. Comment écrire une requête JPQL pour trouver toutes les 
parties où un joueur participe ? Voici mon code actuel qui charge tout en mémoire."
```

**Réponse IA** :
```java
@Query("SELECT g FROM GameEntity g WHERE g.playerIds LIKE CONCAT('%', :playerId, '%')")
List<GameEntity> findByPlayerId(@Param("playerId") String playerId);
```

**Bilan** : ⚠️ Partiellement résolu — la requête fonctionne mais est approximative (`LIKE '%uuid%'` peut matcher un sous-UUID). L'IA n'a pas proposé la solution idéale (table de liaison `@ManyToMany`).

**Limite identifiée** : L'IA propose des solutions "qui fonctionnent" mais pas toujours les plus robustes sur le long terme.

---

### Cas 3 : Configuration Spring Security JWT

**Requête IA** :
```
"Spring Boot 3.5 + Spring Security : j'ai un JwtAuthenticationFilter qui extrait 
le token Bearer et valide la signature. Où dois-je le placer dans la chaîne de filtres ? 
Mon SecurityFilterChain actuel : [code]"
```

**Réponse IA** :
- Proposition 1 : `addFilterBefore(JwtAuthenticationFilter.class, UsernamePasswordAuthenticationFilter.class)`
- Proposition 2 : Utiliser `addFilterAfter()` avec `ExceptionTranslationFilter`

**Bilan** : ✅ Résolu mais avec vérification nécessaire — plusieurs réponses contradictoires selon le modèle IA utilisé. Il a fallu tester les deux configurations pour trouver la bonne.

**Limite identifiée** : Sur les configurations Spring Security, l'IA peut donner des réponses obsolètes (Spring Boot 2.x vs 3.x) ou contradictoires. Il faut toujours vérifier avec la documentation officielle.

---

### Cas 4 : Génération de tests d'intégration

**Requête IA** :
```
"Génère un test d'intégration Spring Boot pour cet endpoint POST /games qui vérifie 
qu'une partie est créée avec le bon statut HTTP et qu'elle est persistée en base. 
Voici le contrôleur : [GameController.java]"
```

**Réponse IA** :
- Génération correcte de `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Utilisation de `TestRestTemplate` avec en-têtes JWT
- Assertions sur le corps de réponse et la base H2

**Bilan** : ✅ Résolu — l'IA est très efficace pour générer des patterns de tests standards.

---

### Synthèse des limites de l'IA sur ce projet : guide pratique par catégorie

#### Tableau récapitulatif

| Type de problème | Efficacité IA | Commentaire |
|------------------|---------------|-------------|
| Annotations Spring standard | ⭐⭐⭐⭐⭐ | Excellente (autowire, repository, service...) |
| Pattern DAO / Repository | ⭐⭐⭐⭐☆ | Très bonne, mais vérifier la compréhension hexagonale |
| Configuration JWT/Security | ⭐⭐⭐☆☆ | Risque d'obsolète ou contradictoire, tester impérativement |
| Tests d'intégration | ⭐⭐⭐⭐⭐ | Excellente pour le boilerplate |
| Architecture microservices | ⭐⭐⭐☆☆ | Bon sur RestClient, moins bon sur subtilités de contrat |
| Refactoring complexe | ⭐⭐☆☆☆ | L'IA peut proposer du code qui compile mais casse la logique métier |
| Requêtes JPQL avancées | ⭐⭐⭐☆☆ | Fonctionnel mais pas toujours optimal (LIKE vs table liaison) |

---

#### 1. Annotations Spring standard — Efficacité ⭐⭐⭐⭐⭐

**Exemple de prompt efficace :**
```
"Dans Spring Boot 3.5, explique la différence entre @Component, @Service, @Repository et @RestController.
Quand utiliser chacun ? Y a-t-il des différences de comportement réel ou est-ce sémantique ?"
```

**Comment utiliser l'IA :**
- Demander une explication conceptuelle avant le code
- Faire vérifier par l'IA si une annotation est dépréciée dans Spring Boot 3.x
- Utiliser pour générer les stéréotypes de base rapidement

**Points de vigilance :**
- ✅ L'IA maîtrise parfaitement les annotations courantes
- ⚠️ Vérifier que les annotations proposées existent bien dans Spring Boot 3.x (pas de `@javax` remplacé par `@jakarta`)
- ⚠️ Sur `@Autowired`, l'IA propose parfois l'injection sur champ (mauvaise pratique) — préférer l'injection par constructeur

---

#### 2. Pattern DAO / Repository — Efficacité ⭐⭐⭐⭐☆

**Exemple de prompt efficace :**
```
"Je dois implémenter le pattern DAO dans une architecture hexagonale Spring Boot.

Contexte du projet SquareGames :
- Interface GameDao dans api/application/
- Implémentation JpaGameDao dans api/infrastructure/
- GameServiceImpl utilise l'interface, pas l'implémentation

Voici GameDao.java : [code]
Voici le début de JpaGameDao.java : [code]

Complète JpaGameDao avec la méthode findByPlayerId(String playerId). 
Important : la méthode doit retourner des objets Game (moteur externe), pas GameEntity."
```

**Comment utiliser l'IA :**
- Fournir explicitement l'arborescence des packages pour forcer le respect de l'architecture
- Indiquer clairement quelle couche contient quoi
- Demander à l'IA de commenter chaque méthode avec sa responsabilité

**Points de vigilance :**
- ⚠️ L'IA tend à regrouper interface + implémentation dans le même package (habitude MVC)
- ⚠️ Vérifier que l'implémentation DAO n'expose pas d'entités JPA à la couche application
- ⚠️ S'assurer que le service dépend de l'interface, jamais de l'implémentation concrète
- ✅ Valider avec `./mvnw test` que le changement de DAO (@Primary) ne casse pas les tests

---

#### 3. Configuration JWT/Security — Efficacité ⭐⭐⭐☆☆

**Exemple de prompt efficace :**
```
"Configuration Spring Security 6.2 + Spring Boot 3.5 pour API stateless JWT.

Contraintes du projet SquareGames :
- Pas de session serveur (stateless)
- Authentification par header Authorization: Bearer <token>
- CSRF désactivé (API REST, pas de formulaire)
- Filtre JWT personnalisé à placer correctement

Voici mon SecurityFilterChain actuel : [code]
Voici mon JwtAuthenticationFilter : [code]

Problème : erreurs 403 sur /games même avec un token valide.
Analyse ce qui ne va pas et propose la correction exacte."
```

**Comment utiliser l'IA :**
- Toujours préciser la version exacte (Spring Boot 3.5, Spring Security 6.2+)
- Fournir le code actuel complet (SecurityFilterChain + Filtre)
- Demander l'explication du placement du filtre AVANT la solution
- Tester chaque proposition immédiatement

**Points de vigilance :**
- 🚨 **CRITIQUE** : L'IA mélange souvent Spring Boot 2.x et 3.x (antMatchers vs requestMatchers, WebSecurityConfigurerAdapter vs SecurityFilterChain)
- 🚨 **CRITIQUE** : Le placement du filtre est crucial — si l'IA propose `addFilterAfter` au lieu de `addFilterBefore`, le filtre JWT ne sera pas exécuté
- ⚠️ Vérifier que `csrf().disable()` est bien justifié (API stateless OK, application web MVC DANGEREUX)
- ⚠️ Toujours valider avec les tests d'intégration (GameControllerIntegrationTest)
- ✅ Vérifier la réponse contre la doc officielle Spring Security 6.2

---

#### 4. Tests d'intégration — Efficacité ⭐⭐⭐⭐⭐

**Exemple de prompt efficace :**
```
"Génère un test d'intégration Spring Boot pour l'endpoint POST /games.

Contexte SquareGames :
- Spring Boot 3.5, Java 21
- Authentification JWT requise (header Authorization: Bearer)
- Renvoie un GameDto avec id, gameType, boardSize, playerIds
- Doit persister en base H2

Structure du projet :
- @SpringBootTest avec WebEnvironment.RANDOM_PORT
- Utilisation de TestRestTemplate
- JWT token à récupérer via /auth/login d'abord

Voici GameController.java : [code]
Voici GameDto.java : [code]

Génère le test complet avec les assertions sur le statut HTTP et le corps de réponse."
```

**Comment utiliser l'IA :**
- Fournir la structure des DTOs et du contrôleur pour des assertions précises
- Indiquer les dépendances (JWT, base de données)
- Demander des assertions explicites sur chaque champ important

**Points de vigilance :**
- ✅ L'IA est très efficace sur le boilerplate des tests (@SpringBootTest, TestRestTemplate)
- ⚠️ Vérifier que l'IA génère bien la récupération du token JWT si l'endpoint est protégé
- ⚠️ S'assurer que les assertions vérifient le comportement métier, pas juste le statut 200
- ✅ Exécuter `./mvnw test` immédiatement pour valider

---

#### 5. Architecture microservices — Efficacité ⭐⭐⭐☆☆

**Exemple de prompt efficace :**
```
"Configuration RestClient Spring Boot 3.2+ pour communication inter-services.

Contexte SquareGames :
- Service api (port 8080) appelle service user-api (port 8081)
- Endpoint à appeler : GET /users/{id}/valid
- Doit gérer les erreurs 404 (utilisateur inexistant) et 503 (service indisponible)
- Configuration via application.properties avec @Value

Voici ce que j'ai commencé : [code]

Complète avec :
1. La configuration RestClient.Builder
2. La méthode de validation avec gestion des erreurs
3. Les tests avec WireMock pour simuler user-api"
```

**Comment utiliser l'IA :**
- Expliquer clairement la topologie des services (qui appelle qui, sur quel port)
- Préciser les codes HTTP attendus et leur signification métier
- Demander la gestion d'erreurs (timeouts, 404, 503)

**Points de vigilance :**
- ⚠️ L'IA propose souvent `RestTemplate` (déprécié) au lieu de `RestClient` (Spring Boot 3.2+)
- ⚠️ La gestion des erreurs est souvent simplifiée — vérifier que les exceptions métier sont bien propagées
- ⚠️ Les tests de contrat WireMock nécessitent une configuration précise que l'IA oublie parfois (`@EnableWireMock`, `@InjectWireMock`)
- ✅ Valider avec `UserValidationContractTest` que tous les cas HTTP sont couverts

---

#### 6. Refactoring complexe — Efficacité ⭐⭐☆☆☆

**Exemple de prompt efficace :**
```
"Refactoring : passer de l'authentification X-UserId (header personnalisé) à JWT (Bearer token).

Contexte SquareGames (Itération 4 vers Itération 5) :
- Avant : header X-UserId passé par le client, validé par appel REST à user-api
- Après : token JWT signé, validation locale par filtre

Code à refactorer :
- GameController.java avec @RequestHeader("X-UserId")
- RestUserValidator qui appelle user-api
- GameServiceImpl qui utilise validateUserId()

Objectifs :
1. Supprimer X-UserId
2. Extraire userId du JWT dans un filtre
3. Stocker dans SecurityContext
4. Récupérer via Authentication dans le contrôleur
5. Supprimer les appels réseau à user-api

Analyse l'impact sur chaque couche (api/application/domain/infrastructure) 
et propose un plan de refactoring étape par étape."
```

**Comment utiliser l'IA :**
- NE JAMAIS demander le refactoring complet en une seule fois
- Découper en petites étapes verifiables (1. Filtre JWT, 2. SecurityContext, 3. Contrôleur, 4. Nettoyage)
- Demander l'analyse d'impact AVANT la solution
- Tester après CHAQUE étape avec `./mvnw test`

**Points de vigilance :**
- 🚨 **CRITIQUE** : L'IA peut proposer du code qui compile mais casse la logique métier
- 🚨 **CRITIQUE** : Dans ce projet, l'IA a proposé de chercher le token dans `getBoard()` au lieu de `getRemainingTokens()` → bug fonctionnel détecté par les tests
- ⚠️ Toujours conserver une branche Git avant refactoring majeur
- ⚠️ Ne pas supprimer l'ancien code avant de valider le nouveau (utiliser @Deprecated d'abord)
- ✅ Les tests d'intégration (Golden Master) sont la seule protection fiable

---

#### 7. Requêtes JPQL avancées — Efficacité ⭐⭐⭐☆☆

**Exemple de prompt efficace :**
```
"Requête JPQL pour trouver les parties d'un joueur.

Contexte SquareGames :
- GameEntity avec champ playerIds (String) contenant "uuid1,uuid2,uuid3"
- Besoin : trouver toutes les parties où un joueur participe
- Actuel : findAll() puis filtre en Java (inefficace)

Problème : playerIds est une String concaténée, pas une relation @ManyToMany

Voici GameEntity.java : [code]

Propose une requête JPQL optimisée. Analyse aussi pourquoi une table de liaison 
serait préférable et ce que ça impliquerait comme refactoring."
```

**Comment utiliser l'IA :**
- Fournir le modèle entité complet
- Demander l'analyse des alternatives (requête JPQL vs refactoring du modèle)
- Faire expliquer les limites de chaque solution

**Points de vigilance :**
- ⚠️ L'IA propose souvent des solutions "quick fix" qui fonctionnent mais créent de la dette technique
- ⚠️ Le `LIKE '%uuid%'` proposé par l'IA est approximatif (peut matcher un sous-UUID)
- ⚠️ L'IA sous-estime souvent l'impact des N+1 queries sur les performances
- ✅ Évaluer le refactoring du modèle (@ManyToMany) même si l'IA ne le propose pas spontanément
- ✅ Tester avec un volume de données réaliste (pas seulement 2-3 parties)

---

#### Tableau récapitulatif : méthodes et vigilance

| Catégorie | Méthode recommandée | Point de vigilance critique |
|-----------|---------------------|----------------------------|
| Annotations Spring | Demander explications + génération | Vérifier versions (@jakarta vs @javax) |
| Pattern DAO | Fournir arborescence complète | Vérifier séparation interface/implémentation |
| JWT/Security | Toujours préciser version Spring Boot 3.5+ | Placement du filtre (before/after), vérifier doc officielle |
| Tests intégration | Fournir DTOs pour assertions précises | Vérifier authentification JWT dans les tests |
| Microservices | Expliquer topologie complète | RestClient vs RestTemplate déprécié |
| Refactoring complexe | Découper en étapes, tester entre chaque | Code qui compile ≠ code correct (bug silencieux) |
| JPQL avancé | Demander analyse des alternatives | Quick fix LIKE vs solution robuste table liaison |

---

## Phase 3 — Méthodologie d'assistance IA pour Spring Boot

### Méthodologie en 6 étapes

#### Étape 1 : Contextualiser le projet
Toujours fournir le contexte architectural au début de la conversation :
```
"Je travaille sur un projet Spring Boot 3.5 avec architecture hexagonale 
(api/application/domain/infrastructure). Voici la structure de ma feature game/ : [arborescence]"
```

**Pourquoi** : L'IA adapte ses réponses à l'organisation par couches plutôt que de générer du MVC classique.

---

#### Étape 2 : Cibler la difficulté précisément
Formuler la requête avec :
- Le code concerné (minimisé à l'essentiel)
- Le message d'erreur exact (stacktrace)
- Ce qui a déjà été tenté

**Exemple efficace** :
```
"Erreur : InvalidDefinitionException on GameEntity.tokens (failed to lazily initialize). 
Entité : [GameEntity.java]. Code d'accès : [JpaGameDao.java ligne 45]. 
J'ai essayé @Eager mais ça charge trop de données."
```

---

#### Étape 3 : Demander l'explication avant le code
Demander à l'IA d'expliquer le "pourquoi" avant le "comment" :
```
"Explique-moi d'abord pourquoi cette erreur se produit (lazy loading), 
puis propose une correction."
```

**Pourquoi** : Cela permet de vérifier que l'IA a bien compris le contexte avant d'appliquer une solution.

---

#### Étape 4 : Valider la réponse avant implémentation
Vérifier la réponse IA contre ces critères :
- [ ] La solution respecte-t-elle l'architecture hexagonale (pas de fuite infrastructure → application) ?
- [ ] Y a-t-il des `@Autowired` sur les champs (à éviter, préférer constructeur) ?
- [ ] La solution est-elle compatible Spring Boot 3.x (pas d'annotations dépréciées) ?
- [ ] Y a-t-il des fuites de sécurité (secrets en dur, CSRF désactivé sans raison) ?

---

#### Étape 5 : Tester avec le Golden Master
Avant d'accepter définitivement une solution IA :
```bash
./mvnw test -q
```

Si un test échoue, retour à l'étape 2 avec le nouveau message d'erreur.

---

#### Étape 6 : Documenter la limite
Pour chaque difficulté résolue par l'IA, noter :
- Ce qui a fonctionné
- Ce qui a été incorrect ou dangereux dans la réponse
- La source de vérification finale (doc Spring, code existant, test)

**Exemple de documentation** :
```markdown
## IA — Configuration JWT
- ✅ Fonctionné : structure du filter, extraction Bearer token
- ⚠️ Incorrect : placement du filter (proposé after, correct est before)
- Vérification : documentation Spring Security 6.2 + test d'intégration
```

---

## Phase 4 — Éléments pour le débat

### Sur quels problèmes Spring Boot l'IA a-t-elle été la plus fiable ?

**Fiable** :
- Syntaxe des annotations Spring (`@Component`, `@Service`, `@Repository`, `@RestController`)
- Génération de DTOs records Java
- Création de repositories Spring Data JPA
- Tests unitaires avec Mockito
- Configuration de base `application.properties`

**Moins fiable** :
- Configuration Spring Security avancée (filtres personnalisés, chaîne de filtres)
- Requêtes JPQL complexes avec jointures
- Architecture hexagonale spécifique (tendance à proposer du MVC classique)
- Gestion des transactions distribuées (microservices)

### Une réponse IA peut-elle remplacer la documentation officielle Spring ?

**Non**, pour ces raisons :
1. **Versionning** : l'IA peut mélanger Spring Boot 2.x et 3.x
2. **Sécurité** : l'IA ne connaît pas les CVE récentes ni les meilleures pratiques de sécurité évolutives
3. **Comportements de bord** : la doc officielle documente les edge cases, l'IA donne le cas nominal

**Usage recommandé** : L'IA pour démarrer rapidement + la doc officielle pour valider et affiner.

### Comment détecter qu'une configuration de sécurité générée par IA est dangereuse ?

**Checklist de vérification** :
- [ ] `csrf().disable()` est-il justifié ? (OK pour API stateless JWT, DANGEREUX pour web MVC)
- [ ] `permitAll()` sur `/admin/**` ou `/actuator/**` ? → Refuser immédiatement
- [ ] Secrets codés en dur dans le code ? → Doit être en `application.properties`
- [ ] `@PreAuthorize` sur tous les endpoints sensibles ?
- [ ] Filtre JWT placé correctement (avant `UsernamePasswordAuthenticationFilter`)

### Risques d'une dépendance excessive à l'IA en contexte professionnel

| Risque | Exemple concret | Mitigation |
|--------|-----------------|------------|
| Auto-cohérence | L'IA génère un test qui passe avec son propre code incorrect | Toujours valider avec les tests existants (Golden Master) |
| Perte de compréhension | Accepter du code JWT sans comprendre la signature/validation | Demander l'explication avant le code |
| Dette technique cachée | Générer du code qui compile mais crée des N+1 queries en production | Revue de code par pair, tests de performance |
| Homogénéisation | Tous les projets IA se ressemblent, perte d'architecture métier | Adapter les réponses au contexte spécifique |

### Comment utiliser l'IA pour progresser en Java sans déléguer la compréhension ?

**Approche pédagogique** :
1. **Étude** : Lire la réponse IA en entier, identifier les concepts nouveaux
2. **Explication** : Reformuler à voix haute (ou à l'écrit) ce que fait chaque ligne
3. **Questionnement** : Demander à l'IA "pourquoi cette ligne ?" sur ce qui est flou
4. **Adaptation** : Modifier la réponse pour l'adapter au projet existant (pas de copier-coller pur)
5. **Documentation** : Noter dans un fichier personnel ce qui a été appris

**Exemple pratique** :
```
IA : "Voici un JwtAuthenticationFilter..."
Moi : "Pourquoi extends OncePerRequestFilter et pas Filter ?"
IA : "OncePerRequestFilter garantit une seule exécution par requête..."
Moi : (note mentale) → À documenter dans mes fiches Spring Security
```

---

## Références

- [AGENTS.md](/home/user/Documents/JavaSpring_Project/AGENTS.md) — Règles du projet pour l'IA
- [audit-technique.md](/home/user/Documents/JavaSpring_Project/docs/audit-technique.md) — Points forts et améliorations du projet
- [README.md](/home/user/Documents/JavaSpring_Project/README.md) — Architecture et démarrage
