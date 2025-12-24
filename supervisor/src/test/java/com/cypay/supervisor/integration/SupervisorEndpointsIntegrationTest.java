package com.cypay.supervisor.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SupervisorEndpointsIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void supervisionEtNotification() {
        // Simulation d'une notification d'erreur
        String notifPayload = "{\"type\":\"ERROR\",\"message\":\"Test supervision\"}";
        ResponseEntity<String> notifResp = restTemplate.postForEntity("/api/supervisor/notify", notifPayload, String.class);
        assertTrue(notifResp.getStatusCode().is2xxSuccessful());
        // Vérifier la prise en compte de la notification (ex: log, base, etc.)
    }
}
