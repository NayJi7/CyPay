
# CyPay – Plateforme d’acteurs distribués pour la gestion de transactions cryptos

## Présentation générale
CyPay est une plateforme distribuée, modulaire et scalable, permettant la gestion de transactions de cryptomonnaies, l’orchestration de portefeuilles, la supervision, la traçabilité et la résilience, inspirée des architectures d’acteurs (type Akka) et des microservices modernes.

Le projet répond à l’ensemble des attendus d’un framework d’acteurs avancé : supervision, scalabilité dynamique, robustesse, traçabilité, sécurité, modularité, et tests d’intégration bout-en-bout.

---

## Architecture globale
- **Microservices** : chaque domaine fonctionnel (transactions, wallet, user, logs, supervisor) est un service Spring Boot indépendant.
- **Framework d’acteurs** : cœur du projet, il permet la création, la supervision et la communication asynchrone entre agents (acteurs).
- **Pools dynamiques** : chaque agent critique (Buy, Sell, Transfer, Blockchain) est géré par un pool qui ajuste dynamiquement le nombre d’instances selon la charge.
- **Supervision centralisée** : tous les événements, erreurs et logs transitent par un superviseur et un logger dédiés, assurant la traçabilité et la résilience.
- **Communication** : les agents communiquent via des messages typés, les microservices via HTTP/REST.

---

## Fonctionnalités principales
### 1. Gestion des transactions
- Achat, vente, transfert de cryptos via des agents spécialisés
- Enregistrement blockchain de chaque opération
- Vérification des soldes, gestion des erreurs, rollback en cas d’échec

### 2. Gestion des portefeuilles (wallet)
- Création, crédit, débit de wallets multi-devises
- Endpoints REST sécurisés

### 3. Authentification et sécurité (user)
- Inscription, login, gestion JWT
- Sécurisation des endpoints sensibles

### 4. Logging et traçabilité
- Logger centralisé (console + base de données)
- Logs professionnels ([INFO], [ERROR], etc.), sans emojis
- Traçabilité complète de chaque action, origine, et erreur

### 5. Supervision et résilience
- Superviseur d’agents : restart automatique, notification d’erreur
- Gestion centralisée des exceptions critiques

### 6. Scalabilité dynamique
- Pools d’acteurs dynamiques : adaptation automatique du nombre d’instances selon la charge
- Extensible à tout nouvel agent via un pattern générique

### 7. Tests et validation
- Tests d’intégration pour chaque service
- Tests end-to-end simulant des scénarios réels (achat complet, etc.)

---

## Détail technique et conformité aux attendus
### Framework d’acteurs
- **Acteur** : chaque agent hérite d’une classe Acteur, possède une mailbox, traite les messages de façon asynchrone
- **Supervision** : chaque acteur est supervisé, les erreurs sont notifiées et traitées
- **Logger** : tous les logs passent par ActeurLogger, qui peut écrire en base si configuré
- **Scalabilité** : DynamicActorPool ajuste le nombre d’instances selon la file d’attente

### Microservices
- **Transactions** : orchestre les agents Buy, Sell, Transfer, Blockchain
- **Wallet** : gère les soldes, les opérations de crédit/débit
- **User** : gère l’authentification, la sécurité JWT
- **Logs** : centralise l’écriture et la lecture des logs
- **Supervisor** : reçoit les notifications d’erreur, peut déclencher des actions correctives

### Sécurité
- **JWT** : tous les endpoints critiques sont protégés par un validateur JWT
- **Validation** : chaque opération vérifie les droits et la cohérence métier

### Traçabilité
- **Logs** : chaque action, chaque message, chaque erreur est tracé avec origine, timestamp, et niveau
- **Base de logs** : possibilité d’auditer toutes les actions via la base

### Scalabilité dynamique des agents (actor pool)
- **Pools dynamiques** : chaque agent critique (BuyAgent, SellAgent, TransferAgent, CreateBlockchainAgent) est géré par un pool dynamique qui ajuste automatiquement le nombre d’instances selon la charge (nombre de messages en attente).
- **Principe** :
	- Pool dynamique créé via `DynamicActorPool` (framework)
	- Seuils :
		- `minActors` : nombre minimal d'instances
		- `maxActors` : nombre maximal d'instances
		- `highWatermark` : si la file d'attente totale dépasse ce seuil, on ajoute un acteur
		- `lowWatermark` : si la file descend sous ce seuil, on supprime un acteur (jamais sous le minimum)
- **Utilisation** :
	- Les pools sont injectés dans `SupervisorAgent` (ex : `BuyAgentPool`, `SellAgentPool`, ...)
	- SupervisorAgent route les messages vers le pool correspondant, qui distribue la charge entre les instances
	- Les agents ne sont plus des `@Component` Spring, mais instanciés par le pool avec leurs dépendances via des setters
- **Exemple d'appel** :
```java
@Autowired
private BuyAgentPool buyAgentPool;
...
buyAgentPool.send(buyMessage);
```
- **Extension** :
	1. Créer un pool similaire (`XAgentPool`)
	2. Adapter l'agent pour accepter ses dépendances via des setters
	3. Remplacer l'injection directe de l'agent par le pool dans le superviseur
- **Fichiers concernés** :
	- `framework/src/main/java/com/cypay/framework/acteur/DynamicActorPool.java`
	- `transactions/src/main/java/com/example/transactions/agent/*AgentPool.java`
	- `transactions/src/main/java/com/example/transactions/agent/SupervisorAgent.java`
- **Bénéfices** :
	- Adaptation automatique à la charge
	- Meilleure résilience et performance
	- Facilement extensible à d'autres agents

### Robustesse
- **Restart automatique** : superviseur relance les agents en cas de crash
- **Gestion des erreurs** : rollback, notification, logs critiques

### Tests
- **Tests d’intégration** : chaque service a des tests sur ses endpoints principaux
- **Tests end-to-end** : scénario complet d’achat, de la création d’utilisateur à l’enregistrement blockchain

---

## Exemples d’utilisation
- **Achat de crypto** : POST `/api/transactions/buy` → orchestration agents → débit/crédit wallet → enregistrement blockchain
- **Consultation logs** : GET `/api/logs` → traçabilité complète
- **Supervision** : POST `/api/supervisor/notify` → notification d’erreur, relance d’agent

---

## Conclusion
CyPay est une solution complète, robuste et moderne, répondant à tous les attendus d’un framework d’acteurs distribué : supervision, scalabilité, sécurité, traçabilité, modularité, et validation par les tests.

Pour toute extension ou adaptation, suivre les patterns exposés dans ce README et dans le code source.
