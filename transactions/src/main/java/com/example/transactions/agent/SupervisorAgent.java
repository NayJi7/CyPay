package com.example.transactions.agent;

import com.example.transactions.message.*;

import com.cypay.framework.acteur.ActeurLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component

public class SupervisorAgent {
    private final ActeurLogger logger = new ActeurLogger("SupervisorAgent");

    @Autowired
    private BuyAgentPool buyAgentPool;

    @Autowired
    private SellAgentPool sellAgentPool;

    @Autowired
    private TransferAgentPool transferAgentPool;

    @Autowired
    private OrderAgent orderAgent;

    @Autowired
    private CreateBlockchainAgentPool createBlockchainAgentPool;

    /**
     * Dispatcher - Route les messages vers les agents appropriés
     */
    public void dispatch(Object message) {
        if (message instanceof BuyMessage msg) {
            logger.info("[ROUTING] Acheminement vers BuyAgentPool (scalable)");
            buyAgentPool.send(msg);
        } else if (message instanceof SellMessage msg) {
            logger.info("[ROUTING] Acheminement vers SellAgentPool (scalable)");
            sellAgentPool.send(msg);
        } else if (message instanceof TransferMessage msg) {
            logger.info("[ROUTING] Acheminement vers TransferAgentPool (scalable)");
            transferAgentPool.send(msg);
        } else if (message instanceof OrderMessage msg) {
            logger.info("[ROUTING] Acheminement vers OrderAgent");
            orderAgent.send(msg);
        } else if (message instanceof CreateBlockchainMessage msg) {
            logger.info("[ROUTING] Acheminement vers CreateBlockchainAgentPool (scalable)");
            createBlockchainAgentPool.send(msg);
        } else {
            logger.erreur("[WARN] Type de message inconnu: " + message.getClass().getName(), null);
        }
    }
}
