package com.cypay.framework.acteur;

import com.cypay.framework.security.JwtValidator;

/**
 * Acteur spécialisé dans la validation des tokens JWT
 */
public class ActeurJwtValidator {

    private final String nom;
    private final ActeurLogger logger;
    private final JwtValidator jwtValidator;

    public ActeurJwtValidator(String nom, String secret, long expirationTimeMs) {
        this.nom = nom;
        this.logger = new ActeurLogger(nom, false); // ✅ Import direct
        this.jwtValidator = new JwtValidator(secret, expirationTimeMs);
    }

    /**
     * Valide un token JWT
     */
    public boolean validerToken(String token) {
        try {
            boolean isValid = jwtValidator.validateToken(token);
            if (isValid) {
                logger.info("[SUCCESS] Token validé avec succès");
            } else {
                logger.erreur("[ERROR] Token invalide ou expiré", new Exception("Token invalide"));
            }
            return isValid;
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la validation du token", e);
            return false;
        }
    }

    /**
     * Extrait l'email du token
     */
    public String extraireEmail(String token) {
        try {
            String email = jwtValidator.extractEmail(token);
            logger.info("[INFO] Email extrait du token : " + email);
            return email;
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de l'extraction de l'email", e);
            return null;
        }
    }

    /**
     * Valide un token pour un utilisateur spécifique
     */
    public boolean validerTokenPourUtilisateur(String token, String email) {
        try {
            boolean isValid = jwtValidator.validateTokenForUser(token, email);
            if (isValid) {
                logger.info("[SUCCESS] Token validé pour l'utilisateur : " + email);
            } else {
                logger.erreur("[ERROR] Token invalide pour l'utilisateur : " + email, new Exception("Token invalide"));
            }
            return isValid;
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la validation du token pour l'utilisateur", e);
            return false;
        }
    }

    /**
     * Extrait le token du header Authorization
     */
    public String extraireTokenDepuisHeader(String authorizationHeader) {
        String token = jwtValidator.extractTokenFromHeader(authorizationHeader);
        if (token != null) {
            logger.info("[INFO] Token extrait du header");
        } else {
            logger.erreur("[ERROR] Aucun token trouvé dans le header", new Exception("Token absent"));
        }
        return token;
    }

    /**
     * Génère un nouveau token
     */
    public String genererToken(String email) {
        try {
            String token = jwtValidator.generateToken(email);
            logger.info("[SUCCESS] Token généré pour : " + email);
            return token;
        } catch (Exception e) {
            logger.erreur("[ERROR] Erreur lors de la génération du token", e);
            return null;
        }
    }

    public String getNom() {
        return nom;
    }

    public ActeurLogger getLogger() {
        return logger;
    }
}