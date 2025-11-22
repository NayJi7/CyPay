package com.cypay.framework.http;

import com.cypay.framework.acteur.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * ✅ HttpReceiver amélioré du framework
 * Supporte 2 modes:
 * 1. Mode simple: envoie HttpIncomingMessage (comme avant)
 * 2. Mode avancé: callback avec HttpExchange pour contrôle total
 */
public class HttpReceiver {

    private HttpServer server;
    private HttpRequestHandler handler;

    /**
     * Interface pour gérer les requêtes HTTP de manière flexible
     */
    public interface HttpRequestHandler {
        void handle(HttpExchange exchange, String method, String path, String query, String body);
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Démarrage avec handler personnalisé
     * Pour applications avancées (Full Acteur)
     */
    public void start(int port, HttpRequestHandler handler) {
        this.handler = handler;

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", this::handleRequestAdvanced);
            server.setExecutor(null);
            server.start();

            System.out.println("✅ Serveur HTTP démarré sur le port " + port + " (mode avancé)");

        } catch (IOException e) {
            System.err.println("❌ Impossible de démarrer le serveur HTTP: " + e.getMessage());
        }
    }

    /**
     * ✅ ANCIENNE MÉTHODE : Démarrage avec acteur (rétrocompatible)
     * Pour applications simples
     */
    public void start(int port, Acteur<?> acteur) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", exchange -> handleRequestSimple(exchange, acteur));
            server.setExecutor(null);
            server.start();

            System.out.println("✅ Serveur HTTP démarré sur le port " + port + " (mode simple)");

        } catch (IOException e) {
            System.err.println("❌ Impossible de démarrer le serveur HTTP: " + e.getMessage());
        }
    }

    /**
     * Gestion avancée avec callback
     */
    private void handleRequestAdvanced(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            InputStream bodyStream = exchange.getRequestBody();
            String body = new String(bodyStream.readAllBytes());

            // ✅ Délègue au handler personnalisé
            if (handler != null) {
                handler.handle(exchange, method, path, query, body);
            } else {
                sendDefaultResponse(exchange);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur traitement requête: " + e.getMessage());
            sendErrorResponse(exchange, 500);
        }
    }

    /**
     * Gestion simple (ancien comportement)
     */
    private void handleRequestSimple(HttpExchange exchange, Acteur<?> acteur) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        InputStream bodyStream = exchange.getRequestBody();
        String body = new String(bodyStream.readAllBytes());

        // ✅ Crée le message et l'envoie à l'acteur
        HttpIncomingMessage msg = new HttpIncomingMessage(method, path, query, body, null);
        acteur.envoyerObjet(msg);

        // Réponse simple par défaut
        String response = "Request received";
        exchange.sendResponseHeaders(202, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * Réponse par défaut
     */
    private void sendDefaultResponse(HttpExchange exchange) {
        try {
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e) {
            System.err.println("❌ Erreur envoi réponse: " + e.getMessage());
        }
    }

    /**
     * Réponse d'erreur
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode) {
        try {
            String response = "Internal Server Error";
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e) {
            System.err.println("❌ Erreur envoi réponse erreur: " + e.getMessage());
        }
    }

    /**
     * Arrête le serveur
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Serveur HTTP arrêté");
        }
    }
}