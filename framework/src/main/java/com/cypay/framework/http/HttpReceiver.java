package com.cypay.framework.http;

import com.cypay.framework.http.HttpIncomingMessage;
import com.cypay.framework.acteur.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HttpReceiver {

    private HttpServer server;

    public void start(int port, Acteur<?> acteur) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/", exchange -> handleRequest(exchange, acteur));

            server.setExecutor(null);
            server.start();

            System.out.printf("Serveur HTTP Acteur lancé sur le port : " + port);

        } catch (IOException e) {
            System.out.printf("Impossible de démarrer le serveur HTTP");
        }
    }
    private void handleRequest(HttpExchange exchange, Acteur<?> acteur) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().toString();
        String body = new String(exchange.getRequestBody().readAllBytes());

        HttpIncomingMessage msg = new HttpIncomingMessage(method, path, body);

        // ✅ Utiliser la méthode non-typée
        acteur.envoyerObjet(msg);

        String response = "Request received";
        exchange.sendResponseHeaders(202, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public void stop() {
        if (server != null)
            server.stop(0);
    }
}
