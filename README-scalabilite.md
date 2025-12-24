# Scalabilité dynamique des agents (actor pool)

## Principe
Chaque agent critique (BuyAgent, SellAgent, TransferAgent, CreateBlockchainAgent) est désormais géré par un pool dynamique qui ajuste automatiquement le nombre d'instances selon la charge (nombre de messages en attente).

- **Pool dynamique** : créé via `DynamicActorPool` (framework)
- **Seuils** :
  - `minActors` : nombre minimal d'instances
  - `maxActors` : nombre maximal d'instances
  - `highWatermark` : si la file d'attente totale dépasse ce seuil, on ajoute un acteur
  - `lowWatermark` : si la file descend sous ce seuil, on supprime un acteur (jamais sous le minimum)

## Utilisation
- Les pools sont injectés dans `SupervisorAgent` (ex : `BuyAgentPool`, `SellAgentPool`, ...)
- SupervisorAgent route les messages vers le pool correspondant, qui distribue la charge entre les instances
- Les agents ne sont plus des `@Component` Spring, mais instanciés par le pool avec leurs dépendances via des setters

## Exemple d'appel
```java
@Autowired
private BuyAgentPool buyAgentPool;
...
buyAgentPool.send(buyMessage);
```

## Extension
Pour ajouter la scalabilité à un nouvel agent :
1. Créer un pool similaire (`XAgentPool`)
2. Adapter l'agent pour accepter ses dépendances via des setters
3. Remplacer l'injection directe de l'agent par le pool dans le superviseur

## Fichiers concernés
- `framework/src/main/java/com/cypay/framework/acteur/DynamicActorPool.java`
- `transactions/src/main/java/com/example/transactions/agent/*AgentPool.java`
- `transactions/src/main/java/com/example/transactions/agent/SupervisorAgent.java`

## Bénéfices
- Adaptation automatique à la charge
- Meilleure résilience et performance
- Facilement extensible à d'autres agents
