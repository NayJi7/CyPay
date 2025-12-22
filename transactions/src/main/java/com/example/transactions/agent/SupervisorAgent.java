package com.example.transactions.agent;

import com.example.transactions.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupervisorAgent {

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
            System.out.println("SupervisorAgent: Routing BuyMessage to BuyAgent");
            buyAgent.send(msg);
        } else if (message instanceof SellMessage msg) {
            System.out.println("SupervisorAgent: Routing SellMessage to SellAgent");
            sellAgent.send(msg);
        } else if (message instanceof TransferMessage msg) {
            System.out.println("SupervisorAgent: Routing TransferMessage to TransferAgent");
            transferAgent.send(msg);
        } else if (message instanceof OrderMessage msg) {
            System.out.println("SupervisorAgent: Routing OrderMessage to OrderAgent");
            orderAgent.send(msg);
        } else if (message instanceof CreateBlockchainMessage msg) {
            System.out.println("SupervisorAgent: Routing CreateBlockchainMessage to CreateBlockchainAgent");
            createBlockchainAgent.send(msg);
        } else {
            System.err.println("SupervisorAgent: Unknown message type: " + message.getClass().getName());
        }
    }
}
