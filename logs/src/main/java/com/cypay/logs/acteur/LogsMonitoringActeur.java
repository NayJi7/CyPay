package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpReceiver;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

/**
 * ✅ Acteur de monitoring exposant un endpoint HTTP pour la supervision
 * Accessible sur un port séparé (ex: 9090)
 */
public class LogsMonitoringActeur extends Acteur<Object> {

    private final SupervisorActeur Supervisor;
    private final Gson gson;
    private HttpReceiver httpReceiver;

    public LogsMonitoringActeur(SupervisorActeur Supervisor) {
        super("MonitoringActeur");
        this.Supervisor = Supervisor;
        this.gson = new Gson();
    }

    /**
     * Démarre le serveur HTTP de monitoring
     */
    public void startMonitoring(int port) {
        httpReceiver = new HttpReceiver();
        httpReceiver.start(port, this::handleMonitoringRequest);
        log("🔍 Serveur de monitoring démarré sur le port " + port);
    }

    /**
     * Gère les requêtes HTTP de monitoring
     */
    private void handleMonitoringRequest(HttpExchange exchange, String method, String path, String query, String body) {
        try {
            log("📊 " + method + " " + path);

            switch (path) {
                case "/health" -> handleHealth(exchange);
                case "/stats" -> handleStats(exchange);
                case "/restart" -> handleRestart(exchange, query);
                case "/shutdown" -> handleShutdown(exchange);
                default -> sendJson(exchange, 404, new ErrorResponse("Endpoint not found"));
            }

        } catch (Exception e) {
            logErreur("💥 Erreur traitement requête monitoring", e);
            sendJson(exchange, 500, new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    /**
     * GET /health - Vérification de santé
     */
    private void handleHealth(HttpExchange exchange) {
        Supervisor.envoyerObjet(new SupervisorActeur.HealthCheckRequest());
        sendJson(exchange, 200, new SuccessResponse("Health check triggered"));
    }

    /**
     * GET /stats - Statistiques du système
     */
    private void handleStats(HttpExchange exchange) {
        Supervisor.envoyerObjet(new SupervisorActeur.GetStatsRequest());
        sendJson(exchange, 200, new SuccessResponse("Statistics generated (check logs)"));
    }

    /**
     * POST /restart?actor=ActorName - Redémarrage d'un acteur
     */
    private void handleRestart(HttpExchange exchange, String query) {
        if (query == null || !query.startsWith("actor=")) {
            sendJson(exchange, 400, new ErrorResponse("Missing 'actor' parameter"));
            return;
        }

        String actorName = query.substring(6); // Retire "actor="
        Supervisor.envoyerObjet(new SupervisorActeur.RestartActorRequest(actorName));

        sendJson(exchange, 200, new SuccessResponse("Restart triggered for: " + actorName));
    }

    /**
     * POST /shutdown - Arrêt du système
     */
    private void handleShutdown(HttpExchange exchange) {
        sendJson(exchange, 200, new SuccessResponse("Shutdown initiated"));

        // Envoyer la commande d'arrêt après avoir répondu
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Supervisor.envoyerObjet(new SupervisorActeur.ShutdownRequest());
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

    public void stopMonitoring() {
        if (httpReceiver != null) {
            httpReceiver.stop();
            log("🛑 Serveur de monitoring arrêté");
        }
    }

    @Override
    protected void traiterMessage(Object message) {
        // Pas utilisé en mode HTTP synchrone
        log("⚠️ Message reçu mais non géré : " + message);
    }

    // ========== DTOs ==========

    private record SuccessResponse(String message) {}
    private record ErrorResponse(String error) {}
}