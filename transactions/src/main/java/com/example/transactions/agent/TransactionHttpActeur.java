package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpReceiver;
import com.example.transactions.message.BuyMessage;
import com.example.transactions.message.SellMessage;
import com.example.transactions.model.CryptoUnit;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class TransactionHttpActeur extends Acteur<Object> {

    private final SupervisorAgent supervisorAgent;
    private final Gson gson;
    private HttpReceiver httpReceiver;

    @Autowired
    public TransactionHttpActeur(SupervisorAgent supervisorAgent) {
        super("TransactionHttpActeur");
        this.supervisorAgent = supervisorAgent;
        this.gson = new Gson();
    }

    public void startHttpServer(int port) {
        httpReceiver = new HttpReceiver();
        httpReceiver.start(port, this::handleHttpRequest);
        log("🌐 Serveur HTTP Transactions démarré sur le port " + port);
    }

    public void stopHttpServer() {
        if (httpReceiver != null) {
            httpReceiver.stop();
            log("🛑 Serveur HTTP Transactions arrêté");
        }
    }

    private void handleHttpRequest(HttpExchange exchange, String method, String path, String query, String body) {
        try {
            log("📨 " + method + " " + path);

            if ("POST".equals(method)) {
                if ("/transactions/buy".equals(path)) {
                    handleBuy(exchange, body);
                    return;
                } else if ("/transactions/sell".equals(path)) {
                    handleSell(exchange, body);
                    return;
                }
            }

            sendError(exchange, 404, "Not found");

        } catch (Exception e) {
            logErreur("💥 Erreur traitement requête HTTP", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleBuy(HttpExchange exchange, String body) {
        try {
            BuyRequest request = gson.fromJson(body, BuyRequest.class);
            BuyMessage message = new BuyMessage(request.userId, request.cryptoUnit, request.amount, request.paymentUnit);
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Achat de " + request.amount + " " + request.cryptoUnit + " pour l'utilisateur " + request.userId + " en cours..."));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request: " + e.getMessage());
        }
    }

    private void handleSell(HttpExchange exchange, String body) {
        try {
            SellRequest request = gson.fromJson(body, SellRequest.class);
            SellMessage message = new SellMessage(request.userId, request.cryptoUnit, request.amount, request.targetUnit);
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Vente de " + request.amount + " " + request.cryptoUnit + " pour l'utilisateur " + request.userId + " en cours..."));
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid request: " + e.getMessage());
        }
    }

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

    private void sendError(HttpExchange exchange, int statusCode, String message) {
        ErrorResponse error = new ErrorResponse(message);
        sendJson(exchange, statusCode, error);
    }

    @Override
    protected void traiterMessage(Object message) {
        log("⚠️ Message reçu mais non géré : " + message);
    }

    private static class BuyRequest {
        Long userId;
        CryptoUnit cryptoUnit;
        Double amount;
        CryptoUnit paymentUnit;
    }

    private static class SellRequest {
        Long userId;
        CryptoUnit cryptoUnit;
        Double amount;
        CryptoUnit targetUnit;
    }

    private static class MessageResponse {
        String message;
        MessageResponse(String message) { this.message = message; }
    }

    private static class ErrorResponse {
        String error;
        ErrorResponse(String error) { this.error = error; }
    }
}
