package com.example.user.utils;

import com.cypay.framework.acteur.ActeurJwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final ActeurJwtValidator jwtActeur;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        // ✅ Utilise l'acteur du framework
        this.jwtActeur = new ActeurJwtValidator("JwtUtil", secret, expiration);
    }

    public String generateToken(String email) {
        return jwtActeur.genererToken(email);
    }

    public boolean validateToken(String token, String email) {
        return jwtActeur.validerTokenPourUtilisateur(token, email);
    }

    public String extractEmail(String token) {
        return jwtActeur.extraireEmail(token);
    }
}