package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;
import com.cypay.framework.http.HttpIncomingMessage;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ✅ Acteur HTTP principal
 * Reçoit les requêtes HTTP et route vers les acteurs appropriés
 */
public class LogHttpActeur extends Acteur<Object> {

    private final DatabaseActeur databaseActeur;
    private final StatsActeur statsActeur;
    private final Gson gson;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogHttpActeur(DatabaseActeur databaseActeur, StatsActeur statsActeur) {
        super("LogHttpActeur");
        this.logger = new ActeurLogger(
                "LogHttpActeur",
                true,
                "jdbc:postgresql://db.yldotyunksweuovyknzg.supabase.co:5432/postgres",
                "postgres",
                "Cypay.Cytech"
        );
        this.databaseActeur = databaseActeur;
        this.statsActeur = statsActeur;

        // ✅ Configuration Gson avec support LocalDateTime
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                context.serialize(src.format(FORMATTER)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                                LocalDateTime.parse(json.getAsString(), FORMATTER))
                .create();
    }

    @Override
    protected void traiterMessage(Object message) {
        if (message instanceof HttpIncomingMessage httpMsg) {
            handleHttpIncoming(httpMsg);

        } else if (message instanceof Messages.LogsResponse response) {
            handleLogsResponse(response);

        } else if (message instanceof Messages.DeleteResponse response) {
            handleDeleteResponse(response);

        } else if (message instanceof Messages.StatsResponse response) {
            handleStatsResponse(response);

        } else if (message instanceof Messages.ErrorResponse error) {
            handleErrorResponse(error);

        } else {
            getLogger().info("❌ Message non reconnu : " + message.getClass().getSimpleName());
        }
    }

    /**
     * Traite les requêtes HTTP entrantes (venant de HttpReceiver)
     */
    private void handleHttpIncoming(HttpIncomingMessage httpMsg) {
        getLogger().info("📨 Requête HTTP reçue : " + httpMsg.method() + " " + httpMsg.path());
    }

    /**
     * Point d'entrée principal pour les requêtes HTTP
     * Appelé directement depuis HttpReceiver modifié
     */
    public void handleHttpRequest(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String requestId = UUID.randomUUID().toString();

        getLogger().info("📨 " + method + " " + path +
                (query != null ? "?" + query : ""));

        try {
            if (method.equals("GET")) {
                routeGetRequest(exchange, path, query, requestId);

            } else if (method.equals("DELETE")) {
                routeDeleteRequest(exchange, path, requestId);

            } else {
                sendError(exchange, "Méthode non supportée: " + method, 405);
            }
        } catch (Exception e) {
            getLogger().erreur("Erreur traitement requête", e);
            sendError(exchange, "Erreur serveur: " + e.getMessage(), 500);
        }
    }

    /**
     * Route les requêtes GET
     */
    private void routeGetRequest(HttpExchange exchange, String path,
                                 String query, String requestId) {
        Map<String, String> params = parseQueryParams(query);
        Integer limit = params.containsKey("limit") ?
                Integer.parseInt(params.get("limit")) : 100;

        if (path.equals("/logs") || path.equals("/logs/all")) {
            // GET /logs?limit=50
            envoyerVers(databaseActeur, new Messages.GetAllLogsQuery(
                    this, requestId, exchange, limit
            ));

        } else if (path.startsWith("/logs/actor/")) {
            // GET /logs/actor/PaymentProcessor?limit=20
            String acteur = path.substring("/logs/actor/".length());
            envoyerVers(databaseActeur, new Messages.GetLogsByActorQuery(
                    this, requestId, exchange, acteur, limit
            ));

        } else if (path.startsWith("/logs/level/")) {
            // GET /logs/level/ERROR?limit=30
            String niveau = path.substring("/logs/level/".length()).toUpperCase();
            envoyerVers(databaseActeur, new Messages.GetLogsByLevelQuery(
                    this, requestId, exchange, niveau, limit
            ));

        } else if (path.equals("/logs/stats")) {
            // GET /logs/stats
            envoyerVers(statsActeur, new Messages.GetStatsQuery(
                    this, requestId, exchange
            ));

        } else {
            sendError(exchange, "Route inconnue: " + path, 404);
        }
    }

    /**
     * Route les requêtes DELETE
     */
    private void routeDeleteRequest(HttpExchange exchange, String path,
                                    String requestId) {
        if (path.equals("/logs")) {
            // DELETE /logs
            envoyerVers(databaseActeur, new Messages.DeleteAllLogsCommand(
                    this, requestId, exchange
            ));
        } else {
            sendError(exchange, "Route DELETE inconnue: " + path, 404);
        }
    }

    /**
     * Traite la réponse avec une liste de logs
     */
    private void handleLogsResponse(Messages.LogsResponse response) {
        if (!response.success()) {
            sendError(response.exchange(), response.error(), 500);
            return;
        }

        getLogger().info("✅ Réponse logs reçue : " + response.logs().size() + " entrées");

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("count", response.logs().size());
        jsonResponse.put("logs", response.logs());

        sendJsonResponse(response.exchange(), jsonResponse, 200);
    }

    /**
     * Traite la réponse de suppression
     */
    private void handleDeleteResponse(Messages.DeleteResponse response) {
        if (!response.success()) {
            sendError(response.exchange(), response.error(), 500);
            return;
        }

        getLogger().info("✅ Suppression effectuée : " + response.deletedCount() + " logs");

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("message", "Logs supprimés avec succès");
        jsonResponse.put("deleted", response.deletedCount());

        sendJsonResponse(response.exchange(), jsonResponse, 200);
    }

    /**
     * Traite la réponse des statistiques
     */
    private void handleStatsResponse(Messages.StatsResponse response) {
        if (!response.success()) {
            sendError(response.exchange(), response.error(), 500);
            return;
        }

        getLogger().info("✅ Statistiques calculées");

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("stats", response.stats());

        sendJsonResponse(response.exchange(), jsonResponse, 200);
    }

    /**
     * Traite une réponse d'erreur
     */
    private void handleErrorResponse(Messages.ErrorResponse error) {
        sendError(error.exchange(), error.error(), error.statusCode());
    }

    /**
     * Envoie une réponse JSON
     */
    private void sendJsonResponse(HttpExchange exchange, Object data, int statusCode) {
        try {
            String json = gson.toJson(data);
            byte[] response = json.getBytes("UTF-8");

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, response.length);

            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();

            getLogger().info("📤 Réponse envoyée : " + statusCode + " (" + response.length + " bytes)");

        } catch (IOException e) {
            getLogger().erreur("Erreur envoi réponse", e);
        }
    }

    /**
     * Envoie une réponse d'erreur
     */
    private void sendError(HttpExchange exchange, String message, int statusCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("code", statusCode);

        sendJsonResponse(exchange, error, statusCode);
    }

    /**
     * Parse les paramètres de requête
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }

        return params;
    }
}