package com.example.user.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserEndpointsIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void inscriptionEtAuthentification() {
        // Exemple d'appel POST pour inscription
        String registerPayload = "{\"username\":\"testuser\",\"password\":\"testpass\"}";
        ResponseEntity<String> registerResp = restTemplate.postForEntity("/api/users/register", registerPayload, String.class);
        assertTrue(registerResp.getStatusCode().is2xxSuccessful());
        // Exemple d'appel POST pour login
        String loginPayload = "{\"username\":\"testuser\",\"password\":\"testpass\"}";
        ResponseEntity<String> loginResp = restTemplate.postForEntity("/api/users/login", loginPayload, String.class);
        assertTrue(loginResp.getStatusCode().is2xxSuccessful());
        // Vérifier la présence d'un token JWT dans la réponse
        assertTrue(loginResp.getBody().contains("token"));
    }
}
