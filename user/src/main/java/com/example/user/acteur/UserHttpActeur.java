package com.example.user.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurJwtValidator;
import com.cypay.framework.http.HttpReceiver;
import com.example.user.service.UserService;
import com.example.user.model.User;
import com.example.user.exception.*;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ✅ Acteur HTTP qui gère toutes les routes du microservice User
 * Utilise HttpReceiver en mode avancé pour un contrôle total
 */
public class UserHttpActeur extends Acteur<Object> {

    private final UserService userService;
    private final ActeurJwtValidator jwtValidator;
    private final Gson gson;
    private HttpReceiver httpReceiver;

    public UserHttpActeur(UserService userService, String jwtSecret, long jwtExpiration) {
        super("UserHttpActeur");
        this.userService = userService;
        this.jwtValidator = new ActeurJwtValidator("JwtValidator", jwtSecret, jwtExpiration);
        this.gson = new Gson();
    }

    /**
     * Démarre le serveur HTTP sur le port spécifié
     */
    public void startHttpServer(int port) {
        httpReceiver = new HttpReceiver();
        httpReceiver.start(port, this::handleHttpRequest);
        log("🌐 Serveur HTTP démarré sur le port " + port);
    }

    /**
     * Point d'entrée pour toutes les requêtes HTTP
     */
    private void handleHttpRequest(HttpExchange exchange, String method, String path, String query, String body) {
        try {
            log("📨 " + method + " " + path);

            // Routage des endpoints
            switch (path) {
                case "/users/register" -> handleRegister(exchange, body);
                case "/users/login" -> handleLogin(exchange, body);
                case "/users/me" -> {
                    if ("GET".equals(method)) handleGetProfile(exchange);
                    else if ("PUT".equals(method)) handleUpdateProfile(exchange, body);
                    else if ("DELETE".equals(method)) handleDeleteProfile(exchange);
                    else sendError(exchange, 405, "Method not allowed");
                }
                case "/users" -> {
                    if ("GET".equals(method)) handleGetAllUsers(exchange);
                    else sendError(exchange, 405, "Method not allowed");
                }
                default -> {
                    if (path.startsWith("/users/")) {
                        String[] parts = path.split("/");
                        if (parts.length == 3) {
                            try {
                                Long userId = Long.parseLong(parts[2]);
                                handleGetUserById(exchange, userId);
                            } catch (NumberFormatException e) {
                                sendError(exchange, 400, "Invalid user ID");
                            }
                        } else {
                            sendError(exchange, 404, "Not found");
                        }
                    } else {
                        sendError(exchange, 404, "Not found");
                    }
                }
            }

        } catch (Exception e) {
            logErreur("💥 Erreur traitement requête HTTP", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // ========== REGISTER ==========

    private void handleRegister(HttpExchange exchange, String body) {
        try {
            RegisterRequest request = gson.fromJson(body, RegisterRequest.class);

            // Validation
            if (request.pseudo == null || request.pseudo.length() < 3 || request.pseudo.length() > 50) {
                sendError(exchange, 400, "Pseudo must be between 3 and 50 characters");
                return;
            }
            if (request.email == null || !request.email.contains("@")) {
                sendError(exchange, 400, "Invalid email");
                return;
            }
            if (request.password == null || request.password.length() < 6) {
                sendError(exchange, 400, "Password must be at least 6 characters");
                return;
            }

            User user = userService.createUser(request.pseudo, request.email, request.password);

            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getPseudo(),
                    user.getEmail(),
                    "User created successfully"
            );

            log("✅ Utilisateur créé : " + user.getEmail());
            sendJson(exchange, 201, response);

        } catch (EmailAlreadyExistsException e) {
            logErreur("❌ Email déjà existant", e);
            sendError(exchange, 409, "Email already exists");
        } catch (Exception e) {
            logErreur("❌ Erreur inscription", e);
            sendError(exchange, 500, "Error creating user: " + e.getMessage());
        }
    }

    // ========== LOGIN ==========

    private void handleLogin(HttpExchange exchange, String body) {
        try {
            LoginRequest request = gson.fromJson(body, LoginRequest.class);

            if (request.email == null || request.password == null) {
                sendError(exchange, 400, "Email and password are required");
                return;
            }

            User user = userService.authenticate(request.email, request.password);
            String token = jwtValidator.genererToken(user.getEmail());

            LoginResponse response = new LoginResponse(
                    token,
                    user.getId(),
                    user.getPseudo(),
                    user.getEmail(),
                    3600
            );

            log("✅ Connexion réussie : " + user.getEmail());
            sendJson(exchange, 200, response);

        } catch (InvalidCredentialsException e) {
            logErreur("❌ Identifiants invalides", e);
            sendError(exchange, 401, "Invalid email or password");
        } catch (Exception e) {
            logErreur("❌ Erreur login", e);
            sendError(exchange, 500, "Login error: " + e.getMessage());
        }
    }

    // ========== GET PROFILE ==========

    private void handleGetProfile(HttpExchange exchange) {
        try {
            String email = extractEmailFromToken(exchange);
            if (email == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            User user = userService.findByEmail(email);
            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getPseudo(),
                    user.getEmail(),
                    null
            );

            log("✅ Profil récupéré : " + user.getEmail());
            sendJson(exchange, 200, response);

        } catch (UserNotFoundException e) {
            logErreur("❌ Utilisateur non trouvé", e);
            sendError(exchange, 404, "User not found");
        } catch (Exception e) {
            logErreur("❌ Erreur récupération profil", e);
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // ========== UPDATE PROFILE ==========

    private void handleUpdateProfile(HttpExchange exchange, String body) {
        try {
            String email = extractEmailFromToken(exchange);
            if (email == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            UpdateUserRequest request = gson.fromJson(body, UpdateUserRequest.class);
            User currentUser = userService.findByEmail(email);

            User updatedUser = userService.updateUser(
                    currentUser.getId(),
                    request.pseudo,
                    request.email,
                    request.password
            );

            UserResponse response = new UserResponse(
                    updatedUser.getId(),
                    updatedUser.getPseudo(),
                    updatedUser.getEmail(),
                    "User updated successfully"
            );

            log("✅ Profil mis à jour : " + updatedUser.getEmail());
            sendJson(exchange, 200, response);

        } catch (UserNotFoundException e) {
            logErreur("❌ Utilisateur non trouvé", e);
            sendError(exchange, 404, "User not found");
        } catch (EmailAlreadyExistsException e) {
            logErreur("❌ Email déjà existant", e);
            sendError(exchange, 409, "Email already exists");
        } catch (Exception e) {
            logErreur("❌ Erreur mise à jour", e);
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // ========== DELETE PROFILE ==========

    private void handleDeleteProfile(HttpExchange exchange) {
        try {
            String email = extractEmailFromToken(exchange);
            if (email == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            User currentUser = userService.findByEmail(email);
            userService.deleteUser(currentUser.getId());

            MessageResponse response = new MessageResponse("User deleted successfully");

            log("✅ Utilisateur supprimé : " + email);
            sendJson(exchange, 200, response);

        } catch (UserNotFoundException e) {
            logErreur("❌ Utilisateur non trouvé", e);
            sendError(exchange, 404, "User not found");
        } catch (Exception e) {
            logErreur("❌ Erreur suppression", e);
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // ========== GET ALL USERS ==========

    private void handleGetAllUsers(HttpExchange exchange) {
        try {
            String email = extractEmailFromToken(exchange);
            if (email == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            List<User> users = userService.findAll();
            List<UserSummary> summaries = users.stream()
                    .map(u -> new UserSummary(u.getId(), u.getPseudo()))
                    .collect(Collectors.toList());

            log("✅ Liste utilisateurs : " + summaries.size() + " résultats");
            sendJson(exchange, 200, summaries);

        } catch (Exception e) {
            logErreur("❌ Erreur récupération utilisateurs", e);
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // ========== GET USER BY ID ==========

    private void handleGetUserById(HttpExchange exchange, Long userId) {
        try {
            String email = extractEmailFromToken(exchange);
            if (email == null) {
                sendError(exchange, 401, "Unauthorized");
                return;
            }

            User user = userService.findById(userId);
            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getPseudo(),
                    user.getEmail(),
                    null
            );

            log("✅ Utilisateur récupéré : ID=" + userId);
            sendJson(exchange, 200, response);

        } catch (UserNotFoundException e) {
            logErreur("❌ Utilisateur non trouvé : ID=" + userId, e);
            sendError(exchange, 404, "User not found with ID: " + userId);
        } catch (Exception e) {
            logErreur("❌ Erreur récupération utilisateur", e);
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // ========== UTILITAIRES ==========

    private String extractEmailFromToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.validerToken(token)) {
            return null;
        }

        return jwtValidator.extraireEmail(token);
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
        // Pas utilisé en mode HTTP synchrone
        log("⚠️ Message reçu mais non géré : " + message);
    }

    public void stopHttpServer() {
        if (httpReceiver != null) {
            httpReceiver.stop();
            log("🛑 Serveur HTTP arrêté");
        }
    }

    // ========== DTOs ==========

    private static class RegisterRequest {
        String pseudo;
        String email;
        String password;
    }

    private static class LoginRequest {
        String email;
        String password;
    }

    private static class UpdateUserRequest {
        String pseudo;
        String email;
        String password;
    }

    private static class UserResponse {
        Long id;
        String pseudo;
        String email;
        String message;

        UserResponse(Long id, String pseudo, String email, String message) {
            this.id = id;
            this.pseudo = pseudo;
            this.email = email;
            this.message = message;
        }
    }

    private static class LoginResponse {
        String token;
        Long userId;
        String pseudo;
        String email;
        int expiresIn;

        LoginResponse(String token, Long userId, String pseudo, String email, int expiresIn) {
            this.token = token;
            this.userId = userId;
            this.pseudo = pseudo;
            this.email = email;
            this.expiresIn = expiresIn;
        }
    }

    private static class MessageResponse {
        String message;

        MessageResponse(String message) {
            this.message = message;
        }
    }

    private static class ErrorResponse {
        String error;

        ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class UserSummary {
        Long id;
        String pseudo;

        UserSummary(Long id, String pseudo) {
            this.id = id;
            this.pseudo = pseudo;
        }
    }
}