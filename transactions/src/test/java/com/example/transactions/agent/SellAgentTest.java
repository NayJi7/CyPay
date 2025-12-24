package com.example.transactions.agent;

import com.example.transactions.message.SellMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class SellAgentTest {
    private SellAgent agent;
    private CreateBlockchainAgent blockchainAgent;
    private com.example.transactions.service.CryptoPriceService priceService;

    @BeforeEach
    void setUp() {
        agent = new SellAgent("jdbc:test", "user", "pass");
        blockchainAgent = mock(CreateBlockchainAgent.class);
        priceService = mock(com.example.transactions.service.CryptoPriceService.class);
        agent.setCreateBlockchainAgent(blockchainAgent);
        agent.setCryptoPriceService(priceService);
        agent.setWalletServiceUrl("http://wallet");
    }

    @Test
    void traiteVenteAvecFondsSuffisants() {
        SellMessage msg = mock(SellMessage.class);
        when(priceService.getPrice(anyString(), anyString())).thenReturn(10000.0);
        // Simuler un solde suffisant
        // ...
        agent.traiterMessage(msg);
        verify(blockchainAgent, atLeastOnce()).send(any());
    }

    @Test
    void traiteVenteAvecFondsInsuffisants() {
        SellMessage msg = mock(SellMessage.class);
        when(priceService.getPrice(anyString(), anyString())).thenReturn(10000.0);
        // Simuler un solde insuffisant
        // ...
        agent.traiterMessage(msg);
        verify(blockchainAgent, never()).send(any());
    }
}
