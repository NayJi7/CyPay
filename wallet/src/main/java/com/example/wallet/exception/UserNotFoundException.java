package com.example.wallet.exception;

/**
 * Exception levée lorsqu'on tente de créer un wallet pour un utilisateur inexistant
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Utilisateur " + userId + " introuvable dans le service User. " +
              "Veuillez d'abord créer l'utilisateur.");
    }
}
