package com.example.transactions.integration;

import com.example.transactions.agent.BuyAgentPool;
import com.example.transactions.agent.SupervisorAgent;
import com.example.transactions.message.BuyMessage;
import com.example.transactions.model.CryptoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BuyFlowIntegrationTest {
    @Autowired
    private SupervisorAgent supervisorAgent;
    @Autowired
    private BuyAgentPool buyAgentPool;

    @Test
    void achatComplet_flow() {
        BuyMessage msg = new BuyMessage(1L, CryptoUnit.BTC, 0.1, CryptoUnit.EUR);
        // Envoi via le superviseur (simulation d'un flux réel)
        assertDoesNotThrow(() -> supervisorAgent.dispatch(msg));
        // Ici, on pourrait vérifier l'état du wallet, la création blockchain, etc.
        // Pour un vrai test, il faudrait mocker les dépendances externes ou utiliser une base de test
    }
}
