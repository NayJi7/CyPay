package com.example.transactions.agent;

import com.example.transactions.message.*;

import com.cypay.framework.acteur.ActeurLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component

public class SupervisorAgent {
    private final ActeurLogger logger = new ActeurLogger("SupervisorAgent");

    @Autowired
    private BuyAgent buyAgent;

    @Autowired
    private SellAgent sellAgent;

    @Autowired
    private TransferAgent transferAgent;

    @Autowired
    private OrderAgent orderAgent;

    @Autowired
    private CreateBlockchainAgent createBlockchainAgent;

    /**
     * Dispatcher - Route les messages vers les agents appropriés
     */
    public void dispatch(Object message) {
        if (message instanceof BuyMessage msg) {
            logger.info("[ROUTING] Acheminement vers BuyAgent");
            buyAgent.send(msg);
        } else if (message instanceof SellMessage msg) {
            logger.info("[ROUTING] Acheminement vers SellAgent");
            sellAgent.send(msg);
        } else if (message instanceof TransferMessage msg) {
            logger.info("[ROUTING] Acheminement vers TransferAgent");
            transferAgent.send(msg);
        } else if (message instanceof OrderMessage msg) {
            logger.info("[ROUTING] Acheminement vers OrderAgent");
            orderAgent.send(msg);
        } else if (message instanceof CreateBlockchainMessage msg) {
            logger.info("[ROUTING] Acheminement vers CreateBlockchainAgent");
            createBlockchainAgent.send(msg);
        } else {
            logger.erreur("[WARN] Type de message inconnu: " + message.getClass().getName(), null);
        }
    }
}
