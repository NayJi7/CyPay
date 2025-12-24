package com.example.transactions.agent;

import com.example.transactions.message.CreateBlockchainMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.transactions.service.DatabaseService;
import static org.mockito.Mockito.*;

class CreateBlockchainAgentTest {
    private CreateBlockchainAgent agent;
    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        agent = new CreateBlockchainAgent("jdbc:test", "user", "pass");
        databaseService = mock(DatabaseService.class);
        agent.setDatabaseService(databaseService);
    }

    @Test
    void traiteAjoutBlockchain() {
        CreateBlockchainMessage msg = mock(CreateBlockchainMessage.class);
        agent.traiterMessage(msg);
        verify(databaseService, atLeastOnce()).saveTransaction(any());
    }
}
