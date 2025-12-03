package com.cypay.supervisor;

/**
 * ✅ Microservice Superviseur Global
 * Surveille tous les microservices du système CyPay
 */
public class SupervisorMain {

    public static void main(String[] args) {

        // Configuration
        int port = 9000;

        // Parser les arguments
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring(7));
            }
        }

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   🛡️  SUPERVISEUR GLOBAL CYPAY               ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();

        // ========== 1. Créer le superviseur global ==========

        System.out.println("📋 Étape 1/3 : Initialisation du superviseur global");
        GlobalSuperviseur superviseur = new GlobalSuperviseur();
        superviseur.demarrer();
        System.out.println("✅ Superviseur global démarré");
        System.out.println();

        // ========== 2. Enregistrer les microservices ==========

        System.out.println("📋 Étape 2/3 : Enregistrement des microservices");

        // Microservice Logs (port 8081, monitoring 9091)
        superviseur.enregistrerMicroservice("logs-service", "localhost", 8081, 9091);

        // Microservice User (port 8082, monitoring 9090)
        superviseur.enregistrerMicroservice("user-service", "localhost", 8082, 9090);

        // Ajouter d'autres microservices ici...
        // superviseur.enregistrerMicroservice("payment-service", "localhost", 8083, 9092);
        // superviseur.enregistrerMicroservice("notification-service", "localhost", 8084, 9093);

        System.out.println();

        // ========== 3. Démarrer l'API HTTP ==========

        System.out.println("📋 Étape 3/3 : Démarrage de l'API HTTP");
        SupervisorHttpActeur httpActeur = new SupervisorHttpActeur(superviseur);
        httpActeur.demarrer();
        httpActeur.startHttpServer(port);
        System.out.println();

        // ========== Affichage des informations ==========

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   ✅ SUPERVISEUR OPÉRATIONNEL                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("🌐 API Superviseur : http://localhost:" + port);
        System.out.println("   GET    /health              - Santé du superviseur");
        System.out.println("   GET    /status              - Statut des microservices");
        System.out.println("   POST   /check               - Health check manuel");
        System.out.println("   POST   /restart?service=X   - Redémarrer un service");
        System.out.println("   POST   /shutdown            - Arrêter le superviseur");
        System.out.println();

        System.out.println("🛡️ Surveillance active :");
        System.out.println("   ✓ Health checks automatiques toutes les 60s");
        System.out.println("   ✓ Alerte après 3 échecs consécutifs");
        System.out.println("   ✓ Microservices surveillés : 2");
        System.out.println();

        System.out.println("📊 Microservices enregistrés :");
        System.out.println("   • logs-service     → http://localhost:8081  (monitoring: 9091)");
        System.out.println("   • user-service     → http://localhost:8082  (monitoring: 9090)");
        System.out.println();

        System.out.println("💡 Exemples de commandes :");
        System.out.println("   # Vérifier le superviseur");
        System.out.println("   curl http://localhost:" + port + "/health");
        System.out.println();
        System.out.println("   # Voir le statut de tous les services");
        System.out.println("   curl http://localhost:" + port + "/status");
        System.out.println();
        System.out.println("   # Déclencher un health check manuel");
        System.out.println("   curl -X POST http://localhost:" + port + "/check");
        System.out.println();
        System.out.println("   # Redémarrer un service");
        System.out.println("   curl -X POST \"http://localhost:" + port + "/restart?service=user-service\"");
        System.out.println();

        System.out.println("🔍 Vérification initiale dans 10 secondes...");
        System.out.println();

        // ========== Shutdown hook ==========

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║   🛑 ARRÊT DU SUPERVISEUR                     ║");
            System.out.println("╚════════════════════════════════════════════════╝");

            httpActeur.stopHttpServer();
            superviseur.envoyerObjet(new GlobalSuperviseur.ShutdownRequest());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("✅ Superviseur arrêté proprement");
        }));

        // ========== Garder le programme actif ==========

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Programme interrompu");
        }
    }
}