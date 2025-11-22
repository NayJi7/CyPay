package com.cypay.logs;

import com.cypay.logs.acteur.*;

/**
 * ✅ Point d'entrée du microservice avec Supervision
 *
 * Architecture:
 *
 * [LogServiceMain]
 *     ↓
 * [SupervisorActeur] 🛡️
 *     ├─ surveille → [DatabaseActeur]
 *     ├─ surveille → [StatsActeur]
 *     └─ surveille → [LogHttpActeur]
 *
 * Si un acteur crash :
 *   Acteur → envoie ActorFailed → Superviseur → décide → Redémarre
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
        System.out.println("🚀 CyPay Log Service - Supervised Actor Model");
        System.out.println("========================================");
        System.out.println("Port      : " + port);
        System.out.println("Database  : " + jdbcUrl);
        System.out.println("========================================\n");

        // ========== Création du SUPERVISEUR ==========

        System.out.println("1️⃣ Création du superviseur...");

        SupervisorActeur supervisor = new SupervisorActeur(
                jdbcUrl,
                dbUser,
                dbPassword,
                SupervisorActeur.SupervisionStrategy.RESTART  // ← Stratégie : redémarrer en cas d'erreur
        );

        supervisor.demarrer();
        System.out.println("   ✅ Superviseur démarré\n");

        // ========== Initialisation des acteurs supervisés ==========

        System.out.println("2️⃣ Initialisation des acteurs supervisés...");

        supervisor.initializeChildren();

        System.out.println("   ✅ 3 acteurs créés et supervisés :");
        System.out.println("      • DatabaseActeur");
        System.out.println("      • StatsActeur");
        System.out.println("      • LogHttpActeur\n");

        // ========== Démarrage du serveur HTTP ==========

        System.out.println("3️⃣ Démarrage du serveur HTTP...");

        com.cypay.framework.http.HttpReceiver httpReceiver =
                new com.cypay.framework.http.HttpReceiver();

        // Handler qui passe directement à l'acteur HTTP via le superviseur
        httpReceiver.start(port, (exchange, method, path, query, body) -> {
            supervisor.getLogHttpActeur().handleHttpRequest(exchange);
        });

        System.out.println();

        // ========== Informations ==========

        System.out.println("========================================");
        System.out.println("✅ Service avec supervision actif !");
        System.out.println("========================================\n");

        System.out.println("🛡️ Supervision active :");
        System.out.println("  • Stratégie        : RESTART (redémarrage auto)");
        System.out.println("  • Max redémarrages : 3 par minute");
        System.out.println("  • Acteurs surveillés : 3");
        System.out.println("  • Health checks    : Disponibles\n");

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

        System.out.println("📊 Architecture supervisée:");
        System.out.println("  [HTTP] → [LogHttpActeur]");
        System.out.println("              ↓");
        System.out.println("          [DatabaseActeur] ← 🛡️ Supervisé");
        System.out.println("          [StatsActeur]    ← 🛡️ Supervisé");
        System.out.println("              ↓");
        System.out.println("          [PostgreSQL]\n");

        System.out.println("🔧 En cas d'erreur:");
        System.out.println("  • L'acteur notifie le superviseur");
        System.out.println("  • Le superviseur redémarre l'acteur");
        System.out.println("  • Le service continue de fonctionner\n");

        System.out.println("Appuyez sur Ctrl+C pour arrêter le service\n");

        // ========== Shutdown hook ==========

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n========================================");
            System.out.println("🛑 Arrêt du service...");
            System.out.println("========================================");

            httpReceiver.stop();
            supervisor.arreter();

            System.out.println("✅ Service arrêté proprement");
        }));

        // ========== Health check périodique (optionnel) ==========

        // Thread de health check toutes les 30 secondes
        Thread healthCheckThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // 30 secondes
                    supervisor.triggerHealthCheck();
                    //supervisor.envoyerVers(supervisor, new Messages.HealthCheck("health-check"));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "HealthCheckThread");
        healthCheckThread.setDaemon(true);
        healthCheckThread.start();

        // ========== Garder le programme actif ==========

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Programme interrompu");
        }
    }
}