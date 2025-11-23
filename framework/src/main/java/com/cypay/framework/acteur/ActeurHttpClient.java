package com.cypay.framework.acteur;

import com.cypay.framework.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

/**
 * Client HTTP intégré pour les acteurs
 * Utilise java.net.http.HttpClient (Java 11+)
 */
public class ActeurHttpClient {

    private final HttpClient client;
    private final ActeurLogger logger;

    public ActeurHttpClient(ActeurLogger logger) {
        this.client = HttpClient.newHttpClient();
        this.logger = logger;
    }

    /**
     * Requête HTTP GET
     *
     * @param url URL complète (ex: "https://api.example.com/users")
     * @return HttpResponse avec status, body, headers
     */
    public HttpResponse get(String url) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            var response = client.send(request, BodyHandlers.ofString());
            logger.httpRequest("GET", url, response.statusCode());
            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());

        } catch (IOException | InterruptedException e) {
            logger.erreur("Erreur lors du GET " + url, e);
            Thread.currentThread().interrupt();
            return new HttpResponse(500, e.getMessage(), Map.of());
        }
    }

    /**
     * Requête HTTP POST
     *
     * @param url URL complète
     * @param jsonBody Corps JSON (peut être null)
     * @return HttpResponse
     */
    public HttpResponse post(String url, String jsonBody) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody))
                    .build();

            var response = client.send(request, BodyHandlers.ofString());
            logger.httpRequest("POST", url, response.statusCode());
            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());

        } catch (IOException | InterruptedException e) {
            logger.erreur("Erreur lors du POST " + url, e);
            Thread.currentThread().interrupt();
            return new HttpResponse(500, e.getMessage(), Map.of());
        }
    }

    /**
     * Requête HTTP PUT
     *
     * @param url URL complète
     * @param jsonBody Corps JSON (peut être null)
     * @return HttpResponse
     */
    public HttpResponse put(String url, String jsonBody) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody))
                    .build();

            var response = client.send(request, BodyHandlers.ofString());
            logger.httpRequest("PUT", url, response.statusCode());
            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());

        } catch (IOException | InterruptedException e) {
            logger.erreur("Erreur lors du PUT " + url, e);
            Thread.currentThread().interrupt();
            return new HttpResponse(500, e.getMessage(), Map.of());
        }
    }

    /**
     * Requête HTTP DELETE
     *
     * @param url URL complète
     * @return HttpResponse
     */
    public HttpResponse delete(String url) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            var response = client.send(request, BodyHandlers.ofString());
            logger.httpRequest("DELETE", url, response.statusCode());
            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());

        } catch (IOException | InterruptedException e) {
            logger.erreur("Erreur lors du DELETE " + url, e);
            Thread.currentThread().interrupt();
            return new HttpResponse(500, e.getMessage(), Map.of());
        }
    }

    /**
     * ✅ Requête HTTP PATCH
     *
     * @param url URL complète
     * @param jsonBody Corps JSON (peut être null)
     * @return HttpResponse
     */
    public HttpResponse patch(String url, String jsonBody) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody))
                    .build();

            var response = client.send(request, BodyHandlers.ofString());
            logger.httpRequest("PATCH", url, response.statusCode());
            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());

        } catch (IOException | InterruptedException e) {
            logger.erreur("Erreur lors du PATCH " + url, e);
            Thread.currentThread().interrupt();
            return new HttpResponse(500, e.getMessage(), Map.of());
        }
    }

    /**
     * Conversion et exécution d'une CustomHttpRequest
     *
     * @param customRequest Requête personnalisée avec headers, méthode, etc.
     * @return HttpResponse
     */
    public HttpResponse execute(CustomHttpRequest customRequest) {
        try {
            // Construction de la requête Java
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(customRequest.getUrl()));

            // Ajout des headers
            if (customRequest.getHeaders() != null) {
                customRequest.getHeaders().forEach(builder::header);
            }

            String method = customRequest.getMethod();
            String body = customRequest.getBody();

            // Sélection de la méthode HTTP
            switch (method) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                default -> {
                    if (body == null)
                        builder.method(method, HttpRequest.BodyPublishers.noBody());
                    else
                        builder.method(method, HttpRequest.BodyPublishers.ofString(body));
                }
            }

            var request = builder.build();
            var response = client.send(request, BodyHandlers.ofString());

            logger.httpRequest(method, customRequest.getUrl(), response.statusCode());
            return new HttpResponse(response.statusCode(), response.body(), response.headers().map());

        } catch (IOException | InterruptedException e) {
            logger.erreur("Erreur lors de l'exécution d'une CustomHttpRequest", e);
            Thread.currentThread().interrupt();
            return new HttpResponse(500, e.getMessage(), Map.of());
        }
    }
}