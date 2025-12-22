# CyPay - Crypto Banking Platform

CyPay est une plateforme bancaire de crypto-monnaie basée sur une architecture micro-services et orientée acteurs.

## 🏗 Architecture

Le projet est divisé en plusieurs modules :

*   **front/** : Application Frontend (React + TypeScript + Tailwind).
*   **framework/** : Librairie partagée contenant les abstractions des Acteurs et utilitaires HTTP.
*   **user/** : Micro-service de gestion des utilisateurs (Auth, Profil). Port **8082**.
*   **wallet/** : Micro-service de gestion des portefeuilles. Port **8083**.
*   **transactions/** : Micro-service de gestion des transactions (Achat/Vente). Port **8081**.
*   **supervisor/** : Superviseur global du système.
*   **logs/** : Service de centralisation des logs.

## 🚀 Prérequis

*   **Java 17** ou supérieur.
*   **Node.js** (v16+) et **npm**.
*   **Maven** (le wrapper `mvnw` est inclus dans chaque module).

## 🛠 Installation et Démarrage

### 1. Installation du Framework (Important)

Le module `framework` est une dépendance pour les autres services. Il doit être installé en premier.

```bash
cd framework
./mvnw clean install
cd ..
```

### 2. Démarrage des Micro-services

Il est recommandé de lancer chaque service dans un terminal séparé.

**Service User (Port 8082)**
```bash
cd user
./mvnw spring-boot:run
```

**Service Wallet (Port 8083)**
```bash
cd wallet
./mvnw spring-boot:run
```

**Service Transactions (Port 8081)**
```bash
cd transactions
./mvnw spring-boot:run
```

**Service Supervisor**
```bash
cd supervisor
./mvnw exec:java -Dexec.mainClass="com.cypay.supervisor.SupervisorMain"
```

**Service Logs**
```bash
cd logs
./mvnw exec:java -Dexec.mainClass="com.cypay.logs.LogServiceMain"
```

### 3. Démarrage du Frontend

Le frontend communique avec les micro-services via un proxy configuré (voir `front/src/setupProxy.js`).

```bash
cd front
npm install
npm start
```

L'application sera accessible sur [http://localhost:3000](http://localhost:3000).

## 📱 Utilisation

1.  **Inscription/Connexion** : Créez un compte depuis la page d'accueil.
2.  **Dashboard** :
    *   Visualisez vos portefeuilles (Crypto & Fiat).
    *   Effectuez des transactions (Achat/Vente de cryptos).
    *   Consultez l'historique de vos opérations.

## ⚙️ Configuration

La configuration de la base de données (PostgreSQL sur Supabase) est centralisée dans les fichiers `application.properties` ou `application.yml` de chaque service.

*   **User** : `user/src/main/resources/application.properties`
*   **Wallet** : `wallet/src/main/resources/application.yml`
*   **Transactions** : `transactions/src/main/resources/application.properties`

## 👥 Auteurs

Projet réalisé dans le cadre du cours de Concepts Avancés de Spring.
