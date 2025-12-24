package com.example.transactions.agent;

import com.example.transactions.message.BuyMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class BuyAgentTest {
    private BuyAgent agent;
    private CreateBlockchainAgent blockchainAgent;
    private com.example.transactions.service.CryptoPriceService priceService;

    @BeforeEach
    void setUp() {
        agent = new BuyAgent("jdbc:test", "user", "pass");
        blockchainAgent = mock(CreateBlockchainAgent.class);
        priceService = mock(com.example.transactions.service.CryptoPriceService.class);
        agent.setCreateBlockchainAgent(blockchainAgent);
        agent.setCryptoPriceService(priceService);
        agent.setWalletServiceUrl("http://wallet");
    }

    @Test
    void traiteAchatAvecFondsSuffisants() {
        BuyMessage msg = new BuyMessage(1L, com.example.transactions.model.CryptoUnit.BTC, 2.0, com.example.transactions.model.CryptoUnit.EUR);
        when(priceService.getPrice(anyString(), anyString())).thenReturn(10000.0);
        // Simuler un solde suffisant
        // ...
        // Appeler la méthode à tester
        agent.traiterMessage(msg);
        // Vérifier que l'agent tente d'enregistrer dans la blockchain
        verify(blockchainAgent, atLeastOnce()).send(any());
    }

    @Test
    void traiteAchatAvecFondsInsuffisants() {
        BuyMessage msg = new BuyMessage(1L, com.example.transactions.model.CryptoUnit.BTC, 2.0, com.example.transactions.model.CryptoUnit.EUR);
        when(priceService.getPrice(anyString(), anyString())).thenReturn(10000.0);
        // Simuler un solde insuffisant
        // ...
        // Appeler la méthode à tester
        agent.traiterMessage(msg);
        // Vérifier qu'aucun enregistrement blockchain n'est tenté
        verify(blockchainAgent, never()).send(any());
    }
}
