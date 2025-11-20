package com.example.wallet.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Client HTTP pour communiquer avec le microservice User
 */
@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${user.service.url:http://localhost:8080}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Vérifie si un utilisateur existe dans le service User
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur existe, false sinon
     */
    public boolean userExists(Long userId) {
        try {
            String url = userServiceUrl + "/users/" + userId;
            restTemplate.getForObject(url, Object.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            // En cas d'erreur de communication, on log mais on laisse passer
            // (pour éviter de bloquer complètement le service si User est down)
            System.err.println("Erreur lors de la vérification de l'utilisateur " + userId + ": " + e.getMessage());
            return true; // Mode dégradé : on autorise
        }
    }
}
