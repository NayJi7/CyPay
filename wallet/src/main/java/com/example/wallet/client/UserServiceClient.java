package com.example.wallet.client;

import com.cypay.framework.acteur.ActeurHttpClient;
import com.cypay.framework.acteur.ActeurLogger;
import com.cypay.framework.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Client HTTP pour communiquer avec le microservice User
 */
@Component
public class UserServiceClient {

    private final ActeurHttpClient httpClient;
    private final String userServiceUrl;

    public UserServiceClient(
            @Value("${user.service.url:http://localhost:8080}") String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
        this.httpClient = new ActeurHttpClient(new ActeurLogger("UserServiceClient"));
    }

    /**
     * Vérifie si un utilisateur existe dans le service User
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur existe, false sinon
     */
    public boolean userExists(Long userId) {
        try {
            String url = userServiceUrl + "/users/" + userId;
            HttpResponse response = httpClient.get(url);
            
            if (response.getStatusCode() == 200) {
                return true;
            } else if (response.getStatusCode() == 404) {
                return false;
            } else {
                // En cas d'erreur autre que 404, on log
                System.err.println("Erreur lors de la vérification de l'utilisateur " + userId + ": Status " + response.getStatusCode());
                return true; // Mode dégradé
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de l'utilisateur " + userId + ": " + e.getMessage());
            return true; // Mode dégradé
        }
    }
}
