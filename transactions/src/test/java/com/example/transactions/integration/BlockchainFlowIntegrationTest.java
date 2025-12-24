package com.example.transactions.integration;

import com.example.transactions.agent.CreateBlockchainAgentPool;
import com.example.transactions.agent.SupervisorAgent;
import com.example.transactions.message.CreateBlockchainMessage;
import com.example.transactions.model.TransactionType;
import com.example.transactions.model.CryptoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BlockchainFlowIntegrationTest {
    @Autowired
    private SupervisorAgent supervisorAgent;
    @Autowired
    private CreateBlockchainAgentPool blockchainAgentPool;

    @Test
    void ajoutBlockchain_flow() {
        CreateBlockchainMessage msg = new CreateBlockchainMessage(TransactionType.BUY, 1L, null, 0.1, CryptoUnit.BTC);
        assertDoesNotThrow(() -> supervisorAgent.dispatch(msg));
        // Vérifier que la transaction est bien enregistrée dans la blockchain (mock ou base de test)
    }
}
