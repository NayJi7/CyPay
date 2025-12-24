package com.example.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AchatCompletEndToEndTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void achatComplet_flow() {
        // 1. Inscription utilisateur
        String registerPayload = "{\"username\":\"e2euser\",\"password\":\"e2epass\"}";
        ResponseEntity<String> registerResp = restTemplate.postForEntity("/api/users/register", registerPayload, String.class);
        assertTrue(registerResp.getStatusCode().is2xxSuccessful());
        // 2. Login utilisateur
        String loginPayload = "{\"username\":\"e2euser\",\"password\":\"e2epass\"}";
        ResponseEntity<String> loginResp = restTemplate.postForEntity("/api/users/login", loginPayload, String.class);
        assertTrue(loginResp.getStatusCode().is2xxSuccessful());
        // 3. Création wallet
        String createWalletPayload = "{\"userId\":1,\"currency\":\"EUR\"}";
        ResponseEntity<String> createWalletResp = restTemplate.postForEntity("/api/wallets", createWalletPayload, String.class);
        assertTrue(createWalletResp.getStatusCode().is2xxSuccessful());
        // 4. Créditer le wallet
        String creditPayload = "{\"currency\":\"EUR\",\"amount\":1000.0}";
        ResponseEntity<String> creditResp = restTemplate.postForEntity("/api/wallets/1/credit", creditPayload, String.class);
        assertTrue(creditResp.getStatusCode().is2xxSuccessful());
        // 5. Achat de crypto (BTC)
        String buyPayload = "{\"userId\":1,\"cryptoUnit\":\"BTC\",\"amount\":0.01,\"paymentUnit\":\"EUR\"}";
        ResponseEntity<String> buyResp = restTemplate.postForEntity("/api/transactions/buy", buyPayload, String.class);
        assertTrue(buyResp.getStatusCode().is2xxSuccessful());
        // 6. Vérification du solde wallet après achat
        ResponseEntity<String> balanceResp = restTemplate.getForEntity("/api/wallets/1/EUR", String.class);
        assertTrue(balanceResp.getStatusCode().is2xxSuccessful());
        // 7. Vérification de la présence de la transaction dans la blockchain
        ResponseEntity<String> blockchainResp = restTemplate.getForEntity("/api/blockchain/user/1", String.class);
        assertTrue(blockchainResp.getStatusCode().is2xxSuccessful());
        assertTrue(blockchainResp.getBody().contains("BTC"));
    }
}
