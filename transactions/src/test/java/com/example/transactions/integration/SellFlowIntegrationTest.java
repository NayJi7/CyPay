package com.example.transactions.integration;

import com.example.transactions.agent.SellAgentPool;
import com.example.transactions.agent.SupervisorAgent;
import com.example.transactions.message.SellMessage;
import com.example.transactions.model.CryptoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SellFlowIntegrationTest {
    @Autowired
    private SupervisorAgent supervisorAgent;
    @Autowired
    private SellAgentPool sellAgentPool;

    @Test
    void venteComplete_flow() {
        SellMessage msg = new SellMessage();
        // Remplir les champs nécessaires du message
        // ...
        assertDoesNotThrow(() -> supervisorAgent.dispatch(msg));
        // Vérifier l'état attendu (wallet, blockchain, etc.)
    }
}
