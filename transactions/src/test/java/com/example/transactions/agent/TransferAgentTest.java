package com.example.transactions.agent;

import com.example.transactions.message.TransferMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class TransferAgentTest {
    private TransferAgent agent;
    private CreateBlockchainAgent blockchainAgent;

    @BeforeEach
    void setUp() {
        agent = new TransferAgent("jdbc:test", "user", "pass");
        blockchainAgent = mock(CreateBlockchainAgent.class);
        agent.setCreateBlockchainAgent(blockchainAgent);
        agent.setWalletServiceUrl("http://wallet");
    }

    @Test
    void traiteTransfertValide() {
        TransferMessage msg = mock(TransferMessage.class);
        // Simuler un transfert valide
        // ...
        agent.traiterMessage(msg);
        verify(blockchainAgent, atLeastOnce()).send(any());
    }

    @Test
    void traiteTransfertInvalide() {
        TransferMessage msg = mock(TransferMessage.class);
        // Simuler un transfert invalide (ex: solde insuffisant)
        // ...
        agent.traiterMessage(msg);
        verify(blockchainAgent, never()).send(any());
    }
}
