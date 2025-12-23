package com.example.wallet.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurJwtValidator;
import com.cypay.framework.http.HttpReceiver;
import com.example.wallet.entity.Wallet;
import com.example.wallet.service.WalletService;
import com.example.wallet.web.dto.CreateWalletRequest;
import com.example.wallet.web.dto.OperationRequest;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class WalletHttpActeur extends Acteur<Object> {

    private final WalletService walletService;
    private final ActeurJwtValidator jwtValidator;
    private final Gson gson;
    private HttpReceiver httpReceiver;

    public WalletHttpActeur(WalletService walletService, String jwtSecret, long jwtExpiration, String jdbcUrl, String dbUser, String dbPassword) {
        super("WalletHttpActeur", true, jdbcUrl, dbUser, dbPassword);
        this.walletService = walletService;
        this.jwtValidator = new ActeurJwtValidator("JwtValidator", jwtSecret, jwtExpiration);
        this.gson = new Gson();
    }

    public void startHttpServer(int port) {
        httpReceiver = new HttpReceiver();
        httpReceiver.start(port, this::handleHttpRequest);
        log("🌐 Serveur HTTP Wallet démarré sur le port " + port);
    }

    public void stopHttpServer() {
        if (httpReceiver != null) {
            httpReceiver.stop();
            log("🛑 Serveur HTTP Wallet arrêté");
        }
    }

    private void handleHttpRequest(HttpExchange exchange, String method, String path, String query, String body) {
        try {
            log("📨 " + method + " " + path);

            if (path.equals("/api/wallets") && "POST".equals(method)) {
                handleCreateWallet(exchange, body);
                return;
            }

            if (path.equals("/api/wallets/transfer") && "POST".equals(method)) {
                handleTransfer(exchange, body);
                return;
            }

            if (path.startsWith("/api/wallets/")) {
                String[] parts = path.split("/");
                // /api/wallets/{userId} -> parts length 4: ["", "api", "wallets", "{userId}"]
                // /api/wallets/{userId}/{currency} -> parts length 5
                // /api/wallets/{userId}/credit -> parts length 5
                
                if (parts.length >= 4) {
                    Long userId;
                    try {
                        userId = Long.parseLong(parts[3]);
                    } catch (NumberFormatException e) {
                        sendError(exchange, 400, "Invalid user ID");
                        return;
                    }

                    if (parts.length == 4) {
                        if ("GET".equals(method)) {
                            handleGetWalletsByUser(exchange, userId);
                            return;
                        } else if ("DELETE".equals(method)) {
                            handleDeleteWallet(exchange, userId); // userId here is actually walletId in this context
                            return;
                        }
                    }

                    if (parts.length == 5) {
                        String lastPart = parts[4];
                        if ("credit".equals(lastPart) && "POST".equals(method)) {
                            handleCredit(exchange, userId, body);
                            return;
                        } else if ("debit".equals(lastPart) && "POST".equals(method)) {
                            handleDebit(exchange, userId, body);
                            return;
                        } else if ("GET".equals(method)) {
                            // Assume lastPart is currency
                            handleGetWallet(exchange, userId, lastPart);
                            return;
                        }
                    }
                }
            }

            sendError(exchange, 404, "Not found");

        } catch (Exception e) {
            logErreur("💥 Erreur traitement requête HTTP", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleCreateWallet(HttpExchange exchange, String body) {
        try {
            CreateWalletRequest request = gson.fromJson(body, CreateWalletRequest.class);
            Wallet wallet = walletService.createWallet(request.getUserId(), request.getCurrency());
            sendJson(exchange, 200, wallet);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetWalletsByUser(HttpExchange exchange, Long userId) {
        try {
            List<Wallet> wallets = walletService.getWalletsByUser(userId);
            sendJson(exchange, 200, wallets);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleGetWallet(HttpExchange exchange, Long userId, String currency) {
        try {
            Wallet wallet = walletService.getWallet(userId, currency);
            sendJson(exchange, 200, wallet);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleCredit(HttpExchange exchange, Long userId, String body) {
        try {
            OperationRequest request = gson.fromJson(body, OperationRequest.class);
            Wallet wallet = walletService.credit(userId, request.getCurrency(), request.getAmount());
            sendJson(exchange, 200, wallet);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleDebit(HttpExchange exchange, Long userId, String body) {
        try {
            OperationRequest request = gson.fromJson(body, OperationRequest.class);
            Wallet wallet = walletService.debit(userId, request.getCurrency(), request.getAmount());
            sendJson(exchange, 200, wallet);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void handleTransfer(HttpExchange exchange, String body) {
        try {
            TransferRequest request = gson.fromJson(body, TransferRequest.class);
            walletService.transfer(request.fromUserId, request.toUserId, request.currency, request.amount);
            sendJson(exchange, 200, new MessageResponse("Transfer successful"));
        } catch (Exception e) {
            logErreur("❌ Erreur handleTransfer", e);
            sendError(exchange, 400, "Transfer failed: " + e.getMessage());
        }
    }

    private void handleDeleteWallet(HttpExchange exchange, Long walletId) {
        try {
            walletService.deleteWallet(walletId);
            sendJson(exchange, 200, new MessageResponse("Wallet deleted successfully"));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
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

    private static class ErrorResponse {
        String error;
        ErrorResponse(String error) { this.error = error; }
    }

    private static class MessageResponse {
        String message;
        MessageResponse(String message) { this.message = message; }
    }

    private static class TransferRequest {
        public Long fromUserId;
        public Long toUserId;
        public String currency;
        public java.math.BigDecimal amount;
    }
}
