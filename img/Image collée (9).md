# Console H2 — Outil de débogage de base de données

> 📸 Référence : `Image collée (9).png` — Console H2 de user-api connectée à `jdbc:h2:mem:userdb`

---

## C'est quoi H2 ?

H2 est une **base de données relationnelle embarquée** écrite en Java. Elle tourne **directement dans la JVM** de ton application Spring Boot, sans installation externe.

- Pas besoin d'installer MySQL, PostgreSQL, etc.
- La base est **en mémoire** (`mem:`) : elle est créée au démarrage et détruite à l'arrêt
- Idéale pour le **développement et les tests**

---

## D'où ça vient dans le projet ?

### 1. La dépendance Maven

Dans `user-api/pom.xml` :
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

Le `scope runtime` signifie : H2 est disponible à l'exécution mais pas à la compilation.

### 2. La configuration

Dans `user-api/src/main/resources/application.properties` :
```properties
spring.datasource.url=jdbc:h2:mem:userdb       ← base en mémoire nommée "userdb"
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true                 ← active l'interface web
spring.h2.console.path=/h2-console             ← URL d'accès
```

### 3. La table USERS

La table est créée automatiquement par **JPA/Hibernate** au démarrage grâce à :
```properties
spring.jpa.hibernate.ddl-auto=update
```

Hibernate lit l'entité `User.java` et génère le SQL correspondant :
```java
@Entity
@Table(name = "users")
public class User {
    @Id private String id;
    @Column private String name;
    @Column(unique = true) private String email;
    @Column private Instant createdAt;
}
```
→ Hibernate crée automatiquement `CREATE TABLE users (id, name, email, created_at)`

---

## Comment accéder à la console

### user-api (port 8081)
```
URL    : http://localhost:8081/h2-console
JDBC URL : jdbc:h2:mem:userdb
User   : sa
Password : (vide)
```

### api de jeux (port 8080)
```
URL    : http://localhost:8080/h2-console
JDBC URL : jdbc:h2:mem:squaregames
User   : sa
Password : (vide)
```

⚠️ **Important** : le champ "JDBC URL" affiché par défaut (`jdbc:h2:~/test`) est incorrect. Il faut le remplacer par `jdbc:h2:mem:userdb` ou `jdbc:h2:mem:squaregames`.

---

## Ce qu'on voit dans l'image

À gauche : l'arborescence de la base `userdb`
- **USERS** → ta table applicative avec les colonnes `ID`, `CREATED_AT`, `EMAIL`, `NAME`
- **INFORMATION_SCHEMA** → métadonnées système (tables, colonnes, contraintes) — pas besoin de toucher à ça

À droite : l'éditeur SQL avec les raccourcis clavier.

---

## Requêtes SQL utiles

### Voir tous les utilisateurs
```sql
SELECT * FROM USERS;
```

### Chercher un utilisateur par email
```sql
SELECT * FROM USERS WHERE EMAIL = 'alice@example.com';
```

### Compter les utilisateurs
```sql
SELECT COUNT(*) FROM USERS;
```

### Voir la structure de la table
```sql
SHOW COLUMNS FROM USERS;
```

### Supprimer tous les utilisateurs (attention !)
```sql
DELETE FROM USERS;
```

Pour exécuter une requête : écris-la dans la zone de texte puis clique sur **Run** (ou `Ctrl+Enter`).

---

## À quoi ça sert concrètement

| Situation | Utilisation |
|-----------|-------------|
| Tu as créé un utilisateur via `POST /users` | Vérifier qu'il est bien en base avec `SELECT * FROM USERS` |
| Un test échoue | Regarder l'état réel de la base |
| Tu veux tester une requête | Écrire du SQL directement sans passer par l'API |
| Tu veux comprendre le schéma | Cliquer sur le nom d'une table pour voir ses colonnes |

---

## Limitations importantes

- ⚠️ **Données perdues à chaque redémarrage** : la base est `mem:` (en mémoire). Si tu stops l'application, tout est effacé.
- ⚠️ **Pas pour la production** : en prod on utilise MySQL, PostgreSQL, etc. (profil `mysql`)
- ⚠️ **Console désactivée en prod** : dans `application-mysql.properties`, `spring.h2.console.enabled=false`

---

## En résumé

```
application.properties
    └─ spring.h2.console.enabled=true
    └─ spring.datasource.url=jdbc:h2:mem:userdb
            │
            ▼
    JPA/Hibernate crée les tables au démarrage
    (à partir des entités @Entity)
            │
            ▼
    Console accessible sur /h2-console
    (outil visuel pour inspecter la base)
```

La console H2 est un **outil de débogage uniquement**. Pour interagir avec l'application, utilise l'API REST (`curl`, Postman, Bruno).
