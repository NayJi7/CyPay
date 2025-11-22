package com.cypay.logs;

import com.cypay.logs.acteur.*;

/**
 * ✅ Point d'entrée du microservice Full Acteur
 *
 * Architecture:
 *
 * [HTTP Request]
 *     ↓
 * [CustomHttpReceiver] (port 8081)
 *     ↓ handleHttpRequest()
 * [LogHttpActeur mailbox]
 *     ↓ traiterMessage() dans thread dédié
 *     ↓ envoyerVers(databaseActeur, query)
 * [DatabaseActeur mailbox]
 *     ↓ traiterMessage() dans thread dédié
 *     ↓ SQL query
 *     ↓ envoyerVers(logHttpActeur, response)
 * [LogHttpActeur mailbox]
 *     ↓ traiterMessage()
 *     ↓ sendJsonResponse()
 * [HTTP Response]
 */
public class LogServiceMain {

    public static void main(String[] args) {

        // Configuration par défaut
        int port = 8081;
        String jdbcUrl = "jdbc:postgresql://db.yldotyunksweuovyknzg.supabase.co:5432/postgres";
        String dbUser = "postgres";
        String dbPassword = "Cypay.Cytech";

        // Parser les arguments
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring(7));
            } else if (arg.startsWith("--db.url=")) {
                jdbcUrl = arg.substring(9);
            } else if (arg.startsWith("--db.user=")) {
                dbUser = arg.substring(10);
            } else if (arg.startsWith("--db.password=")) {
                dbPassword = arg.substring(14);
            }
        }

        System.out.println("========================================");
        System.out.println("🚀 CyPay Log Service - Full Actor Model");
        System.out.println("========================================");
        System.out.println("Port      : " + port);
        System.out.println("Database  : " + jdbcUrl);
        System.out.println("========================================\n");

        // ========== Création des acteurs ==========

        System.out.println("1️⃣ Création des acteurs...");

        // Acteur de base de données
        DatabaseActeur databaseActeur = new DatabaseActeur(jdbcUrl, dbUser, dbPassword);

        // Acteur de statistiques
        StatsActeur statsActeur = new StatsActeur(jdbcUrl, dbUser, dbPassword);

        // Acteur HTTP (dépend des autres)
        LogHttpActeur logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);

        System.out.println("   ✅ 3 acteurs créés\n");

        // ========== Démarrage des acteurs ==========

        System.out.println("2️⃣ Démarrage des acteurs...");

        databaseActeur.demarrer();
        statsActeur.demarrer();
        logHttpActeur.demarrer();

        System.out.println("   ✅ Tous les acteurs démarrés\n");

        // ========== Démarrage du serveur HTTP ==========

        System.out.println("3️⃣ Démarrage du serveur HTTP...");

        // ✅ Utilise le HttpReceiver du framework en mode avancé
        com.cypay.framework.http.HttpReceiver httpReceiver =
                new com.cypay.framework.http.HttpReceiver();

        // Handler qui passe directement à l'acteur
        httpReceiver.start(port, (exchange, method, path, query, body) -> {
            logHttpActeur.handleHttpRequest(exchange);
        });

        System.out.println();

        // ========== Informations ==========

        System.out.println("========================================");
        System.out.println("✅ Service démarré avec succès !");
        System.out.println("========================================\n");

        System.out.println("📚 Endpoints disponibles:");
        System.out.println("  GET    /logs                   → Tous les logs");
        System.out.println("  GET    /logs?limit=50          → Limiter à 50 résultats");
        System.out.println("  GET    /logs/actor/{nom}       → Logs d'un acteur");
        System.out.println("  GET    /logs/level/{niveau}    → Logs par niveau (INFO, ERROR)");
        System.out.println("  GET    /logs/stats             → Statistiques globales");
        System.out.println("  DELETE /logs                   → Supprimer tous les logs\n");

        System.out.println("💡 Exemples de requêtes:");
        System.out.println("  curl http://localhost:" + port + "/logs?limit=10");
        System.out.println("  curl http://localhost:" + port + "/logs/actor/PaymentProcessor");
        System.out.println("  curl http://localhost:" + port + "/logs/level/ERROR");
        System.out.println("  curl http://localhost:" + port + "/logs/stats");
        System.out.println("  curl -X DELETE http://localhost:" + port + "/logs\n");

        System.out.println("📊 Architecture:");
        System.out.println("  [HTTP] → [LogHttpActeur] → [DatabaseActeur] → [PostgreSQL]");
        System.out.println("                          → [StatsActeur] → [PostgreSQL]\n");

        System.out.println("Appuyez sur Ctrl+C pour arrêter le service\n");

        // ========== Shutdown hook ==========

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n========================================");
            System.out.println("🛑 Arrêt du service...");
            System.out.println("========================================");

            httpReceiver.stop();
            logHttpActeur.arreter();
            databaseActeur.arreter();
            statsActeur.arreter();

            System.out.println("✅ Service arrêté proprement");
        }));

        // ========== Garder le programme actif ==========

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Programme interrompu");
        }
    }
}