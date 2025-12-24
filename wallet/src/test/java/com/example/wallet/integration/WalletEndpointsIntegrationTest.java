package com.example.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletEndpointsIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void creationEtDebitCreditWallet() {
        // Création d'un wallet
        String createPayload = "{\"userId\":1,\"currency\":\"EUR\"}";
        ResponseEntity<String> createResp = restTemplate.postForEntity("/api/wallets", createPayload, String.class);
        assertTrue(createResp.getStatusCode().is2xxSuccessful());
        // Créditer le wallet
        String creditPayload = "{\"currency\":\"EUR\",\"amount\":100.0}";
        ResponseEntity<String> creditResp = restTemplate.postForEntity("/api/wallets/1/credit", creditPayload, String.class);
        assertTrue(creditResp.getStatusCode().is2xxSuccessful());
        // Débiter le wallet
        String debitPayload = "{\"currency\":\"EUR\",\"amount\":50.0}";
        ResponseEntity<String> debitResp = restTemplate.postForEntity("/api/wallets/1/debit", debitPayload, String.class);
        assertTrue(debitResp.getStatusCode().is2xxSuccessful());
    }
}
