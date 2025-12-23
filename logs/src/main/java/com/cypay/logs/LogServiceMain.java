package com.cypay.logs;

import com.cypay.logs.acteur.*;
import com.cypay.framework.http.HttpReceiver;

public class LogServiceMain {

    public static void main(String[] args) {
        int port = 8084;
        int monitoringPort = 9091;
        String jdbcUrl = "jdbc:postgresql://aws-1-eu-north-1.pooler.supabase.com:5432/postgres";
        String dbUser = "postgres.yldotyunksweuovyknzg";
        String dbPassword = "Cypay.Cytech";

        // Parser arguments
        for (String arg : args) {
            if (arg.startsWith("--port=")) port = Integer.parseInt(arg.substring(7));
            else if (arg.startsWith("--monitoring.port=")) monitoringPort = Integer.parseInt(arg.substring(18));
            else if (arg.startsWith("--db.url=")) jdbcUrl = arg.substring(9);
            else if (arg.startsWith("--db.user=")) dbUser = arg.substring(10);
            else if (arg.startsWith("--db.password=")) dbPassword = arg.substring(14);
        }

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   🚀 DÉMARRAGE DU SYSTÈME LOGS                ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();

        // 1. Superviseur
        System.out.println("📋 Étape 1/4 : Initialisation du superviseur");
        SupervisorActeur Supervisor = new SupervisorActeur(jdbcUrl, dbUser, dbPassword);
        Supervisor.demarrer();
        System.out.println("✅ Supervisor démarré");
        System.out.println();

        // 2. Acteurs métier
        System.out.println("📋 Étape 2/4 : Initialisation des acteurs métier");
        Supervisor.initializeChildren();
        System.out.println();

        // 3. Monitoring
        System.out.println("📋 Étape 3/4 : Création de l'acteur de monitoring");
        LogsMonitoringActeur monitoringActeur = new LogsMonitoringActeur(Supervisor);
        Supervisor.enregistrerActeur("LogsMonitoringActeur", monitoringActeur);
        Supervisor.demarrerActeur("LogsMonitoringActeur");
        monitoringActeur.startMonitoring(monitoringPort);
        System.out.println();

        // 4. HTTP principal
        System.out.println("📋 Étape 4/4 : Démarrage du serveur HTTP principal");
        HttpReceiver httpReceiver = new HttpReceiver();
        httpReceiver.start(port, (exchange, method, path, query, body) -> {
            Supervisor.getLogHttpActeur().handleHttpRequest(exchange);
        });
        System.out.println("✅ Serveur HTTP démarré sur le port " + port);
        System.out.println();

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   ✅ SYSTÈME OPÉRATIONNEL                     ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("🌐 API Logs : http://localhost:" + port);
        System.out.println("🔍 API Monitoring : http://localhost:" + monitoringPort);
        System.out.println("   GET    /health");
        System.out.println("   GET    /stats");
        System.out.println();

        // Health check initial
        try {
            Thread.sleep(2000);
            Supervisor.envoyerObjet(new SupervisorActeur.HealthCheckRequest());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Arrêt du système...");
            monitoringActeur.stopMonitoring();
            httpReceiver.stop();
            Supervisor.envoyerObjet(new SupervisorActeur.ShutdownRequest());
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("✅ Système arrêté");
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.err.println("Programme interrompu");
        }
    }
}