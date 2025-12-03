package com.cypay.supervisor;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpReceiver;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

/**
 * ✅ Acteur HTTP pour l'API du superviseur global
 */
public class SupervisorHttpActeur extends Acteur<Object> {

    private final GlobalSuperviseur superviseur;
    private final Gson gson;
    private HttpReceiver httpReceiver;

    public SupervisorHttpActeur(GlobalSuperviseur superviseur) {
        super("SupervisorHttpActeur");
        this.superviseur = superviseur;
        this.gson = new Gson();
    }

    /**
     * Démarre le serveur HTTP
     */
    public void startHttpServer(int port) {
        httpReceiver = new HttpReceiver();
        httpReceiver.start(port, this::handleHttpRequest);
        log("🌐 API Superviseur démarrée sur le port " + port);
    }

    /**
     * Gère les requêtes HTTP
     */
    private void handleHttpRequest(HttpExchange exchange, String method, String path, String query, String body) {
        try {
            log("📨 " + method + " " + path);

            switch (path) {
                case "/health" -> handleHealth(exchange);
                case "/status" -> handleStatus(exchange);
                case "/check" -> handleManualCheck(exchange);
                case "/restart" -> handleRestart(exchange, query);
                case "/shutdown" -> handleShutdown(exchange);
                default -> sendJson(exchange, 404, new ErrorResponse("Endpoint not found"));
            }

        } catch (Exception e) {
            logErreur("💥 Erreur traitement requête", e);
            sendJson(exchange, 500, new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * GET /health - Health check du superviseur lui-même
     */
    private void handleHealth(HttpExchange exchange) {
        HealthResponse response = new HealthResponse(
                "ok",
                "Global Supervisor is running",
                System.currentTimeMillis()
        );
        sendJson(exchange, 200, response);
    }

    /**
     * GET /status - Statut de tous les microservices
     */
    private void handleStatus(HttpExchange exchange) {
        superviseur.envoyerObjet(new GlobalSuperviseur.GetStatusRequest());
        sendJson(exchange, 200, new SuccessResponse("Status report generated (check logs)"));
    }

    /**
     * POST /check - Déclenche un health check manuel
     */
    private void handleManualCheck(HttpExchange exchange) {
        superviseur.envoyerObjet(new GlobalSuperviseur.ManualHealthCheckRequest());
        sendJson(exchange, 200, new SuccessResponse("Manual health check triggered"));
    }

    /**
     * POST /restart?service=ServiceName - Redémarre un microservice
     */
    private void handleRestart(HttpExchange exchange, String query) {
        if (query == null || !query.startsWith("service=")) {
            sendJson(exchange, 400, new ErrorResponse("Missing 'service' parameter"));
            return;
        }

        String serviceName = query.substring(8);
        superviseur.envoyerObjet(new GlobalSuperviseur.RestartMicroserviceRequest(serviceName));

        sendJson(exchange, 200, new SuccessResponse("Restart command sent to: " + serviceName));
    }

    /**
     * POST /shutdown - Arrête le superviseur
     */
    private void handleShutdown(HttpExchange exchange) {
        sendJson(exchange, 200, new SuccessResponse("Shutdown initiated"));

        new Thread(() -> {
            try {
                Thread.sleep(500);
                superviseur.envoyerObjet(new GlobalSuperviseur.ShutdownRequest());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Envoie une réponse JSON
     */
    private void sendJson(HttpExchange exchange, int statusCode, Object data) {
        try {
            String json = gson.toJson(data);
            byte[] response = json.getBytes();

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.length);

            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();

        } catch (IOException e) {
            logErreur("❌ Erreur envoi réponse JSON", e);
        }
    }

    public void stopHttpServer() {
        if (httpReceiver != null) {
            httpReceiver.stop();
            log("🛑 Serveur HTTP arrêté");
        }
    }

    @Override
    protected void traiterMessage(Object message) {
        log("⚠️ Message reçu mais non géré : " + message);
    }

    // ========== DTOs ==========

    private record SuccessResponse(String message) {}
    private record ErrorResponse(String error) {}
    private record HealthResponse(String status, String message, long timestamp) {}
}