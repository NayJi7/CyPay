package com.cypay.logs.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LogsEndpointsIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void ecritureEtLectureLog() {
        // Écriture d'un log
        String logPayload = "{\"niveau\":\"INFO\",\"message\":\"Test log\"}";
        ResponseEntity<String> writeResp = restTemplate.postForEntity("/api/logs", logPayload, String.class);
        assertTrue(writeResp.getStatusCode().is2xxSuccessful());
        // Lecture des logs
        ResponseEntity<String> readResp = restTemplate.getForEntity("/api/logs", String.class);
        assertTrue(readResp.getStatusCode().is2xxSuccessful());
        assertTrue(readResp.getBody().contains("Test log"));
    }
}
