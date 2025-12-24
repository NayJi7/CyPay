package com.example.transactions.agent;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpReceiver;
import com.cypay.framework.http.HttpResponse; // Added Import
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
    private final String walletServiceUrl; // Added field
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public TransactionHttpActeur(
            SupervisorAgent supervisorAgent,
            DatabaseService databaseService,
            CryptoPriceService cryptoPriceService,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${wallet.service.url}") String walletServiceUrl // Added parameter
    ) {
        super("TransactionHttpActeur", true, jdbcUrl, dbUser, dbPassword);
        this.supervisorAgent = supervisorAgent;
        this.databaseService = databaseService;
        this.cryptoPriceService = cryptoPriceService;
        this.walletServiceUrl = walletServiceUrl; // Initialize field
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
        logger.info("[HTTP] Serveur HTTP Transactions démarré sur le port " + port);
    }

    public void stopHttpServer() {
        if (httpReceiver != null) {
            httpReceiver.stop();
            logger.info("[HTTP] Serveur HTTP Transactions arrêté");
        }
    }

    private void handleHttpRequest(HttpExchange exchange, String method, String path, String query, String body) {
        try {
            logger.info("[HTTP-REQ] " + method + " " + path);

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
            logger.erreur("[ERROR] Erreur lors du traitement HTTP", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetPrices(HttpExchange exchange) {
        try {
            Map<String, Double> prices = cryptoPriceService.getAllPrices();
            logger.info("[PROCESS] Récupération des prix de toutes les cryptos");
            sendJson(exchange, 200, prices);
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la récupération des prix", e);
            sendError(exchange, 500, "Error fetching prices: " + e.getMessage());
        }
    }

    private void handleGetHistory(HttpExchange exchange, Long userId) {
        try {
            logger.info("[PROCESS] Récupération de l'historique pour userId=" + userId);
            List<Transaction> transactions = databaseService.findUserTransactions(userId);
            sendJson(exchange, 200, transactions);
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la récupération de l'historique", e);
            sendError(exchange, 500, "Error fetching history: " + e.getMessage());
        }
    }

    private void handleBuy(HttpExchange exchange, String body) {
        try {
            logger.info("[IN] Requête d'achat reçue: " + body);
            BuyRequest request = gson.fromJson(body, BuyRequest.class);
            
            if (request.userId == null) {
                logger.erreur("[ERROR] userId manquant dans la requête", null);
                sendError(exchange, 400, "userId is required");
                return;
            }

            // --- Validation Préliminaire du Solde ---
            double prixUnitaire = cryptoPriceService.getPrice(request.cryptoUnit.name(), request.paymentUnit.name());
            double montantAPayer = request.amount * prixUnitaire;
            
            String error = checkBalance(request.userId, request.paymentUnit.name(), montantAPayer);
            if (error != null) {
                logger.erreur("[ERROR] Validation solde échouée: " + error, null);
                sendError(exchange, 400, "Transaction refusée: " + error);
                return;
            }
            // ----------------------------------------

            BuyMessage message = new BuyMessage(request.userId, request.cryptoUnit, request.amount, request.paymentUnit);
            logger.info("[ROUTING] HTTP -> SupervisorAgent (BuyMessage)");
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Achat de " + request.amount + " " + request.cryptoUnit + " pour l'utilisateur " + request.userId + " initié avec succès."));
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur handleBuy", e);
            sendError(exchange, 400, "Invalid request: " + e.getMessage());
        }
    }

    private void handleSell(HttpExchange exchange, String body) {
        try {
            logger.info("[IN] Requête de vente reçue: " + body);
            SellRequest request = gson.fromJson(body, SellRequest.class);
            SellMessage message = new SellMessage(request.userId, request.cryptoUnit, request.amount, request.targetUnit);
            logger.info("[ROUTING] HTTP -> SupervisorAgent (SellMessage)");
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Vente de " + request.amount + " " + request.cryptoUnit + " pour l'utilisateur " + request.userId + " initiée avec succès."));
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur handleSell", e);
            sendError(exchange, 400, "Invalid request: " + e.getMessage());
        }
    }

    private void handleTransfer(HttpExchange exchange, String body) {
        try {
            logger.info("[IN] Requête de virement reçue: " + body);
            TransferRequest request = gson.fromJson(body, TransferRequest.class);
            com.example.transactions.message.TransferMessage message = new com.example.transactions.message.TransferMessage(
                    request.fromUserId,
                    request.toUserId,
                    request.cryptoUnit,
                    request.amount
            );
            logger.info("[ROUTING] HTTP -> SupervisorAgent (TransferMessage)");
            supervisorAgent.dispatch(message);
            sendJson(exchange, 200, new MessageResponse("Virement de " + request.amount + " " + request.cryptoUnit + " de " + request.fromUserId + " vers " + request.toUserId + " initié."));
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur handleTransfer", e);
            sendError(exchange, 400, "Invalid request: " + e.getMessage());
        }
    }

    /**
     * Vérifie si l'utilisateur a assez de fonds.
     * @return null si OK, message d'erreur sinon.
     */
    private String checkBalance(Long userId, String currency, double amountRequired) {
        try {
            String balanceUrl = String.format("%s/api/wallets/%d/%s", walletServiceUrl, userId, currency);
            HttpResponse response = get(balanceUrl);

            if (response.getStatusCode() != 200) {
                return "Portefeuille " + currency + " introuvable.";
            }

            // Parsing simple: {"id":1, ... "balance":1000.0, ...}
            String json = response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> walletData = gson.fromJson(json, Map.class);
            
            if (walletData.containsKey("balance")) {
                double balance = ((Number) walletData.get("balance")).doubleValue();
                if (balance < amountRequired) {
                    return "Solde insuffisant. Requis: " + String.format("%.2f", amountRequired) + " " + currency + ", Dispo: " + String.format("%.2f", balance);
                }
                return null; // OK
            }
            return "Structure de réponse wallet invalide.";

        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur checkBalance", e);
            return "Erreur vérification solde: " + e.getMessage();
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
            logger.erreur("[ERROR] Erreur lors de l'envoi de la réponse JSON", e);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) {
        ErrorResponse error = new ErrorResponse(message);
        sendJson(exchange, statusCode, error);
    }

    @Override
    protected void traiterMessage(Object message) {
        logger.info("[WARN] Message non géré reçu: " + message);
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
