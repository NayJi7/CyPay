package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpReceiver;
import com.example.transactions.message.BuyMessage;
import com.example.transactions.message.SellMessage;
import com.example.transactions.model.CryptoUnit;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

import com.example.transactions.service.DatabaseService;
import com.example.transactions.service.CryptoPriceService;
import com.example.transactions.model.Transaction;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TransactionHttpActeur extends Acteur<Object> {

    private final SupervisorAgent supervisorAgent;
    private final DatabaseService databaseService;
    private final CryptoPriceService cryptoPriceService;
    private final Gson gson;
    private HttpReceiver httpReceiver;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public TransactionHttpActeur(
            SupervisorAgent supervisorAgent,
            DatabaseService databaseService,
            CryptoPriceService cryptoPriceService,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword
    ) {
        super("TransactionHttpActeur", true, jdbcUrl, dbUser, dbPassword);
        this.supervisorAgent = supervisorAgent;
        this.databaseService = databaseService;
        this.cryptoPriceService = cryptoPriceService;
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
                } else if ("/transactions/transfer".equals(path)) {
                    handleTransfer(exchange, body);
                    return;
                }
            } else if ("GET".equals(method)) {
                if (path.startsWith("/transactions/history/")) {
                    String[] parts = path.split("/");
                    if (parts.length == 4) {
                        Long userId = Long.parseLong(parts[3]);
                        handleGetHistory(exchange, userId);
                        return;
                    }
                } else if ("/transactions/prices".equals(path)) {
                    handleGetPrices(exchange);
                    return;
                }
            }

            sendError(exchange, 404, "Not found");

        } catch (Exception e) {
            logErreur("💥 Erreur traitement requête HTTP", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetPrices(HttpExchange exchange) {
        try {
            Map<String, Double> prices = cryptoPriceService.getAllPrices();
            sendJson(exchange, 200, prices);
        } catch (Exception e) {
            logErreur("❌ Erreur handleGetPrices", e);
            sendError(exchange, 500, "Error fetching prices: " + e.getMessage());
        }
    }

    private void handleGetHistory(HttpExchange exchange, Long userId) {
        try {
            List<Transaction> transactions = databaseService.findUserTransactions(userId);
            sendJson(exchange, 200, transactions);
        } catch (Exception e) {
            logErreur("❌ Erreur handleGetHistory", e);
            sendError(exchange, 500, "Error fetching history: " + e.getMessage());
        }
    }

    private void handleBuy(HttpExchange exchange, String body) {
        try {
            log("📥 Body reçu pour achat: " + body);
            BuyRequest request = gson.fromJson(body, BuyRequest.class);
            
            if (request.userId == null) {
                log("❌ Erreur: userId est null dans la requête");
                sendError(exchange, 400, "userId is required");
                return;
            }

            BuyMessage message = new BuyMessage(request.userId, request.cryptoUnit, request.amount, request.paymentUnit);
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Achat de " + request.amount + " " + request.cryptoUnit + " pour l'utilisateur " + request.userId + " en cours..."));
        } catch (Exception e) {
            logErreur("❌ Erreur handleBuy", e);
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

    private void handleTransfer(HttpExchange exchange, String body) {
        try {
            TransferRequest request = gson.fromJson(body, TransferRequest.class);
            // Note: TransferMessage expects (fromUserId, toUserId, amount, cryptoUnit)
            // But TransferRequest might have different field names. Let's define TransferRequest first.
            com.example.transactions.message.TransferMessage message = new com.example.transactions.message.TransferMessage(
                    request.fromUserId,
                    request.toUserId,
                    request.cryptoUnit,
                    request.amount
            );
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Virement de " + request.amount + " " + request.cryptoUnit + " de " + request.fromUserId + " vers " + request.toUserId + " en cours..."));
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
        public Long userId;
        public CryptoUnit cryptoUnit;
        public Double amount;
        public CryptoUnit paymentUnit;
    }

    private static class SellRequest {
        public Long userId;
        public CryptoUnit cryptoUnit;
        public Double amount;
        public CryptoUnit targetUnit;
    }

    private static class TransferRequest {
        public Long fromUserId;
        public Long toUserId;
        public CryptoUnit cryptoUnit;
        public Double amount;
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
