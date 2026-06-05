# Démarrage du projet

Ce document explique comment configurer, lancer et tester le projet sur Fedora, Ubuntu et Windows.

## Prérequis communs

- **Java 21+** (JDK, pas JRE)
- **Maven** (ou utiliser le wrapper `./mvnw` inclus dans chaque application)
- Aucune base de données externe nécessaire : H2 embarqué en fichier est utilisé par défaut

---

## Fedora

### 1. Installer Java 21

```bash
sudo dnf install java-21-openjdk-devel
```

Vérifier l'installation :

```bash
java -version
javac -version
```

### 2. Cloner le dépôt

```bash
git clone https://github.com/dylanholin/JavaSpring_Project.git
cd JavaSpring_Project
```

### 3. Installer le moteur de jeu en local

Le dossier `cda-java-spring-game-engine-main/` ne contient pas de wrapper Maven. Utiliser `mvn` (installation globale) ou le wrapper de l'api :

```bash
cd cda-java-spring-game-engine-main
mvn install
cd ..
```

> 📌 Si `mvn` n'est pas installé, utiliser le wrapper de l'api : `../api_java_3_5/api/mvnw install`

> 📌 Cette étape n'est nécessaire qu'une seule fois.

### 4. Démarrer user-api (port 8081)

```bash
cd user-api
./mvnw spring-boot:run
```

Laisse ce terminal ouvert et ouvre un nouveau terminal pour l'étape suivante.

### 5. Démarrer l'app de jeux (port 8080)

```bash
cd api_java_3_5/api
./mvnw spring-boot:run
```

> ⚠️ Démarrer **user-api en premier** : l'endpoint `/auth/login` (émission des JWT) est sur user-api. L'app de jeux valide les tokens localement mais a besoin de user-api démarré pour que les joueurs puissent se connecter.

### 6. Lancer les tests

```bash
# Tests de l'app de jeux (60 tests)
cd api_java_3_5/api
./mvnw test

# Tests de user-api (14 tests)
cd user-api
./mvnw test
```

---

## Ubuntu

### 1. Installer Java 21

```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

Vérifier l'installation :

```bash
java -version
javac -version
```

### 2. Cloner le dépôt

```bash
git clone https://github.com/dylanholin/JavaSpring_Project.git
cd JavaSpring_Project
```

### 3. Installer le moteur de jeu en local

Le dossier `cda-java-spring-game-engine-main/` ne contient pas de wrapper Maven. Utiliser `mvn` (installation globale) ou le wrapper de l'api :

```bash
cd cda-java-spring-game-engine-main
mvn install
cd ..
```

> 📌 Si `mvn` n'est pas installé, utiliser le wrapper de l'api : `../api_java_3_5/api/mvnw install`

> 📌 Cette étape n'est nécessaire qu'une seule fois.

### 4. Démarrer user-api (port 8081)

```bash
cd user-api
./mvnw spring-boot:run
```

Laisse ce terminal ouvert et ouvre un nouveau terminal pour l'étape suivante.

### 5. Démarrer l'app de jeux (port 8080)

```bash
cd api_java_3_5/api
./mvnw spring-boot:run
```

> ⚠️ Démarrer **user-api en premier** : l'endpoint `/auth/login` (émission des JWT) est sur user-api. L'app de jeux valide les tokens localement mais a besoin de user-api démarré pour que les joueurs puissent se connecter.

### 6. Lancer les tests

```bash
# Tests de l'app de jeux (60 tests)
cd api_java_3_5/api
./mvnw test

# Tests de user-api (14 tests)
cd user-api
./mvnw test
```

---

## Windows

### 1. Installer Java 21+

Télécharger et installer un JDK 21 ou version ultérieure (21, 25, etc.) depuis [adoptium.net](https://adoptium.net/).

### 2. Configurer JAVA_HOME

Le projet nécessite un JDK (pas un JRE). Vérifie la version utilisée par défaut :

```powershell
java -version
```

Si elle affiche une version < 21 (ex: 1.8), ou si `javac -version` est introuvable, configure `JAVA_HOME` :

```powershell
echo $env:JAVA_HOME
```

Si la variable est vide ou pointe vers un JRE, trouve ton JDK :

```powershell
Get-ChildItem "C:\Program Files\Eclipse Adoptium" -ErrorAction SilentlyContinue
Get-ChildItem "C:\Program Files\Java" -ErrorAction SilentlyContinue
```

Définis `JAVA_HOME` et mets à jour le `PATH` pour la session PowerShell :

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

> 📌 Adapte le chemin selon ton installation. Le dossier doit contenir `bin\javac.exe`.

Vérifie que la bonne version est active :

```powershell
java -version
javac -version
```

Pour rendre la variable persistante, ajoute `JAVA_HOME` dans les **variables d'environnement système** de Windows et place `%JAVA_HOME%\bin` au début du `PATH`.

### 3. Cloner le dépôt

```powershell
git clone https://github.com/dylanholin/JavaSpring_Project.git
cd JavaSpring_Project
```

### 4. Installer le moteur de jeu en local

Le moteur de jeu est fourni dans le dépôt mais doit être installé dans le cache Maven local avant de pouvoir builder l'application de jeux :

```powershell
cd cda-java-spring-game-engine-main
& "..\api_java_3_5\api\mvnw.cmd" install
cd ..
```

> 📌 Cette étape n'est nécessaire qu'une seule fois, tant que le cache Maven local n'est pas vidé.

### 5. Démarrer user-api (port 8081)

```powershell
cd user-api
.\mvnw.cmd spring-boot:run
```

Laisse ce terminal ouvert et ouvre un nouveau terminal pour l'étape suivante.

### 6. Démarrer l'app de jeux (port 8080)

```powershell
cd api_java_3_5\api
.\mvnw.cmd spring-boot:run
```

> ⚠️ Démarrer **user-api en premier** : l'endpoint `/auth/login` (émission des JWT) est sur user-api. L'app de jeux valide les tokens localement mais a besoin de user-api démarré pour que les joueurs puissent se connecter.

### 7. Lancer les tests

```powershell
# Tests de l'app de jeux (60 tests)
cd api_java_3_5\api
.\mvnw.cmd test

# Tests de user-api (14 tests)
cd user-api
.\mvnw.cmd test
```

### Dépannage Windows

| Problème | Cause | Solution |
|----------|-------|----------|
| `No compiler is provided` | `JAVA_HOME` pointe vers un JRE | Définir `JAVA_HOME` vers un JDK (voir étape 2) |
| `Could not resolve dependencies: engine` | Le moteur n'est pas dans le cache Maven local | Lancer l'étape 4 (install du moteur) |
| `mvnw.cmd not recognized` | Mauvais répertoire | Vérifier le `cd` avant de lancer la commande |
| `&&` non reconnu dans PowerShell | PowerShell n'accepte pas `&&` | Lancer les commandes séparément |

---

## Utiliser l'API

Une fois les deux applications démarrées, voir le guide complet dans le [README.md](../README.md#jouer-une-partie--guide-complet).

## Déploiement (suggestion)

Aucune configuration Docker ou cloud n'est en place actuellement. Voici un exemple de `docker-compose.yml` suggéré pour déployer les deux services :

```yaml
version: "3.8"
services:
  user-api:
    build: ./user-api
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=h2
  api:
    build: ./api_java_3_5/api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=h2
      - USER_SERVICE_URL=http://user-api:8081
    depends_on:
      - user-api
```

> ⚠️ Ce fichier est une **suggestion** non testée. Pour la production, utiliser le profil MySQL avec une base de données externe et sécuriser les endpoints.
