package com.example.user.controller;

import com.cypay.framework.acteur.ActeurLogger;
import com.example.user.dto.*;
import com.example.user.exception.*;
import com.example.user.model.User;
import com.example.user.service.UserService;
import com.example.user.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final ActeurLogger loggingActeur;

    public UserController(UserService userService, JwtUtil jwtUtil,
                          @Value("${logs.db.enabled:false}") boolean logsDbEnabled,
                          @Value("${logs.db.url:}") String logsDbUrl,
                          @Value("${logs.db.user:}") String logsDbUser,
                          @Value("${logs.db.password:}") String logsDbPassword
                          ) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.loggingActeur = new ActeurLogger(
                "UserController",
                true,
                logsDbUrl,
                logsDbUser,
                logsDbPassword
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.createUser(request.getPseudo(), request.getEmail(), request.getPassword());
            loggingActeur.info("Nouvel utilisateur inscrit : " + user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new UserResponse(user.getId(), user.getPseudo(), user.getEmail(), "User created successfully"));
        } catch (EmailAlreadyExistsException e) {
            loggingActeur.erreur("Échec inscription : email existant", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Email already exists"));
        } catch (Exception e) {
            loggingActeur.erreur("Erreur lors de l'inscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error creating user: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.authenticate(request.getEmail(), request.getPassword());
            String token = jwtUtil.generateToken(user.getEmail());
            loggingActeur.info("Utilisateur connecté : " + user.getEmail());
            return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getPseudo(), user.getEmail(), 3600));
        } catch (InvalidCredentialsException e) {
            loggingActeur.erreur("Connexion échouée pour : " + request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid email or password"));
        } catch (Exception e) {
            loggingActeur.erreur("Erreur login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Login error: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User user = userService.findByEmail(email);
            loggingActeur.info("Profil récupéré : " + user.getEmail());
            return ResponseEntity.ok(new UserResponse(user.getId(), user.getPseudo(), user.getEmail(), null));
        } catch (UserNotFoundException e) {
            loggingActeur.erreur("Utilisateur non trouvé", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User currentUser = userService.findByEmail(email);
            User user = userService.updateUser(currentUser.getId(), request.getPseudo(), request.getEmail(), request.getPassword());
            loggingActeur.info("Profil mis à jour : " + user.getEmail());
            return ResponseEntity.ok(new UserResponse(user.getId(), user.getPseudo(), user.getEmail(), "User updated successfully"));
        } catch (UserNotFoundException e) {
            loggingActeur.erreur("Utilisateur non trouvé pour mise à jour", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        } catch (EmailAlreadyExistsException e) {
            loggingActeur.erreur("Email déjà existant pour mise à jour", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Email already exists"));
        } catch (Exception e) {
            loggingActeur.erreur("Erreur mise à jour utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Error updating user: " + e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            User currentUser = userService.findByEmail(email);
            userService.deleteUser(currentUser.getId());
            loggingActeur.info("Utilisateur supprimé : " + currentUser.getEmail());
            return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
        } catch (UserNotFoundException e) {
            loggingActeur.erreur("Utilisateur non trouvé pour suppression", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        }
    }

    /**
     * Récupère tous les utilisateurs (ID + Pseudo uniquement)
     * Endpoint protégé par JWT
     * GET /users
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.findAll();
            List<UserSummary> userSummaries = users.stream()
                    .map(user -> new UserSummary(user.getId(), user.getPseudo()))
                    .collect(Collectors.toList());
            loggingActeur.info("Liste des utilisateurs récupérée : " + userSummaries.size() + " utilisateurs");
            return ResponseEntity.ok(userSummaries);
        } catch (Exception e) {
            loggingActeur.erreur("Erreur lors de la récupération des utilisateurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching users: " + e.getMessage()));
        }
    }

    /**
     * Récupère le profil d'un utilisateur par son ID
     * Endpoint protégé par JWT
     * GET /users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            loggingActeur.info("Profil récupéré pour l'utilisateur ID : " + id);
            return ResponseEntity.ok(new UserResponse(user.getId(), user.getPseudo(), user.getEmail(), null));
        } catch (UserNotFoundException e) {
            loggingActeur.erreur("Utilisateur non trouvé avec l'ID : " + id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("User not found with ID: " + id));
        } catch (Exception e) {
            loggingActeur.erreur("Erreur lors de la récupération de l'utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching user: " + e.getMessage()));
        }
    }

    // ==================== CLASSES INTERNES ====================

    public static class ErrorResponse {
        private String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }

    public static class MessageResponse {
        private String message;
        public MessageResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    /**
     * DTO pour la liste des utilisateurs (ID + Pseudo uniquement)
     */
    public static class UserSummary {
        private Long id;
        private String pseudo;

        public UserSummary(Long id, String pseudo) {
            this.id = id;
            this.pseudo = pseudo;
        }

        public Long getId() { return id; }
        public String getPseudo() { return pseudo; }
    }
}