package com.cypay.framework;

/**
 * Point d'entrée logique du framework (pas d'exécution).
 * Sert uniquement à fournir des outils aux microservices Spring Boot.
 */
public class FrameworkApplication {

    public static void init() {
        System.out.println("CyPay Framework loaded successfully.");
    }
}
