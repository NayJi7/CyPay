package com.example.transactions.integration;

import com.example.transactions.agent.TransferAgentPool;
import com.example.transactions.agent.SupervisorAgent;
import com.example.transactions.message.TransferMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransferFlowIntegrationTest {
    @Autowired
    private SupervisorAgent supervisorAgent;
    @Autowired
    private TransferAgentPool transferAgentPool;

    @Test
    void transfertComplet_flow() {
        TransferMessage msg = new TransferMessage();
        // Remplir les champs nécessaires du message
        // ...
        assertDoesNotThrow(() -> supervisorAgent.dispatch(msg));
        // Vérifier l'état attendu (wallet, blockchain, etc.)
    }
}
