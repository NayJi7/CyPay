package com.cypay.supervisor;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.http.HttpResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ✅ Superviseur global qui surveille tous les microservices
 * Vérifie régulièrement que chaque microservice est vivant via HTTP
 */
public class GlobalSuperviseur extends Acteur<Object> {

    private final Map<String, MicroserviceInfo> microservices = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Configuration
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 60; // 1 minute
    private static final int HEALTH_CHECK_TIMEOUT_MS = 5000; // 5 secondes
    private static final int MAX_FAILURES_BEFORE_ALERT = 3;

    public GlobalSuperviseur() {
        super("GlobalSuperviseur");
    }

    @Override
    public void demarrer() {
        super.demarrer();

        // Lancer les health checks automatiques
        scheduler.scheduleAtFixedRate(
                this::performGlobalHealthCheck,
                10, // Délai initial de 10 secondes
                HEALTH_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log("✅ Health checks globaux activés (toutes les " + HEALTH_CHECK_INTERVAL_SECONDS + "s)");
    }

    /**
     * Enregistre un microservice à superviser
     */
    public void enregistrerMicroservice(String nom, String host, int port, int monitoringPort) {
        enregistrerMicroservice(nom, host, port, monitoringPort, "/health");
    }

    /**
     * Enregistre un microservice à superviser avec un chemin de health check spécifique
     */
    public void enregistrerMicroservice(String nom, String host, int port, int monitoringPort, String healthPath) {
        MicroserviceInfo info = new MicroserviceInfo(nom, host, port, monitoringPort, healthPath);
        microservices.put(nom, info);
        log("📋 Microservice enregistré : " + nom + " (" + host + ":" + port + ") path=" + healthPath);
    }

    /**
     * Effectue un health check sur tous les microservices
     */
    private void performGlobalHealthCheck() {
        log("🏥 ═══════════════════════════════════════════════");
        log("🏥 HEALTH CHECK GLOBAL - " + LocalDateTime.now().format(formatter));
        log("🏥 ═══════════════════════════════════════════════");

        boolean allHealthy = true;

        for (Map.Entry<String, MicroserviceInfo> entry : microservices.entrySet()) {
            String name = entry.getKey();
            MicroserviceInfo info = entry.getValue();

            boolean isHealthy = checkMicroserviceHealth(info);

            if (isHealthy) {
                info.recordSuccess();
                log(String.format("✅ %-20s : ACTIF (uptime: %s)",
                        name, formatDuration(info.getUptime())));
            } else {
                info.recordFailure();
                allHealthy = false;
                log(String.format("❌ %-20s : DÉFAILLANT (échecs: %d/%d)",
                        name, info.getConsecutiveFailures(), MAX_FAILURES_BEFORE_ALERT));

                // Alerte si trop d'échecs
                if (info.getConsecutiveFailures() >= MAX_FAILURES_BEFORE_ALERT) {
                    log("🚨 ALERTE : " + name + " ne répond plus depuis " +
                            info.getConsecutiveFailures() + " vérifications !");
                }
            }
        }

        log("─────────────────────────────────────────────────");
        log("Statut global : " + (allHealthy ? "✅ TOUS LES SERVICES ACTIFS" : "⚠️ PROBLÈMES DÉTECTÉS"));
        log("═════════════════════════════════════════════════");
        log("");
    }

    /**
     * Vérifie la santé d'un microservice spécifique
     */
    private boolean checkMicroserviceHealth(MicroserviceInfo info) {
        try {
            // Appel HTTP GET vers le health check du monitoring
            String healthUrl = "http://" + info.getHost() + ":" + info.getMonitoringPort() + info.getHealthPath();

            HttpResponse response = get(healthUrl);

            // Considérer comme sain si status 200-299
            return response.isSuccess();

        } catch (Exception e) {
            logErreur("❌ Erreur health check " + info.getName(), e);
            return false;
        }
    }

    @Override
    protected void traiterMessage(Object message) {

        if (message instanceof ManualHealthCheckRequest) {
            performGlobalHealthCheck();

        } else if (message instanceof GetStatusRequest) {
            handleGetStatus();

        } else if (message instanceof RestartMicroserviceRequest req) {
            handleRestartMicroservice(req);

        } else if (message instanceof ShutdownRequest) {
            handleShutdown();

        } else {
            log("⚠️ Message non géré : " + message.getClass().getSimpleName());
        }
    }


    /**
     * Retourne le statut de tous les microservices
     */
    private void handleGetStatus() {
        log("📊 STATUT DES MICROSERVICES");
        log("═════════════════════════════════════════════════");

        for (Map.Entry<String, MicroserviceInfo> entry : microservices.entrySet()) {
            MicroserviceInfo info = entry.getValue();
            log("");
            log("Microservice : " + info.getName());
            log("  Host          : " + info.getHost());
            log("  Port principal: " + info.getPort());
            log("  Port monitoring: " + info.getMonitoringPort());
            log("  État          : " + (info.isHealthy() ? "ACTIF" : "DÉFAILLANT"));
            log("  Échecs consécutifs : " + info.getConsecutiveFailures());
            log("  Total vérifications : " + info.getTotalChecks());
            log("  Uptime        : " + formatDuration(info.getUptime()));
            log("  Dernière vérif: " + info.getLastCheckTime());
        }

        log("═════════════════════════════════════════════════");
    }

    /**
     * Redémarre un microservice via son API de monitoring
     */
    private void handleRestartMicroservice(RestartMicroserviceRequest req) {
        String serviceName = req.serviceName();
        MicroserviceInfo info = microservices.get(serviceName);

        if (info == null) {
            log("❌ Microservice inconnu : " + serviceName);
            return;
        }

        log("🔄 Tentative de redémarrage de : " + serviceName);

        try {
            // Appel POST vers /shutdown du service
            String shutdownUrl = "http://" + info.getHost() + ":" +
                    info.getMonitoringPort() + "/shutdown";

            HttpResponse response = post(shutdownUrl, "");

            if (response.isSuccess()) {
                log("✅ Commande de redémarrage envoyée à " + serviceName);
            } else {
                log("⚠️ Échec de la commande de redémarrage : " + response.getStatusCode());
            }

        } catch (Exception e) {
            logErreur("❌ Erreur redémarrage " + serviceName, e);
        }
    }

    /**
     * Arrêt du superviseur
     */
    private void handleShutdown() {
        log("🛑 ARRÊT DU SUPERVISEUR GLOBAL");

        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        log("✅ Superviseur global arrêté");
        this.arreter();
    }

    /**
     * Formate une durée en millisecondes
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "j " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    // ========== MESSAGES ==========

    public record ManualHealthCheckRequest() {}
    public record GetStatusRequest() {}
    public record RestartMicroserviceRequest(String serviceName) {}
    public record ShutdownRequest() {}

    // ========== INFO MICROSERVICE ==========

    private static class MicroserviceInfo {
        private final String name;
        private final String host;
        private final int port;
        private final int monitoringPort;
        private final String healthPath;
        private final long startTime;

        private boolean healthy;
        private int consecutiveFailures;
        private int totalChecks;
        private LocalDateTime lastCheckTime;

        public MicroserviceInfo(String name, String host, int port, int monitoringPort, String healthPath) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.monitoringPort = monitoringPort;
            this.healthPath = healthPath;
            this.startTime = System.currentTimeMillis();
            this.healthy = true;
            this.consecutiveFailures = 0;
            this.totalChecks = 0;
        }

        public void recordSuccess() {
            this.healthy = true;
            this.consecutiveFailures = 0;
            this.totalChecks++;
            this.lastCheckTime = LocalDateTime.now();
        }

        public void recordFailure() {
            this.healthy = false;
            this.consecutiveFailures++;
            this.totalChecks++;
            this.lastCheckTime = LocalDateTime.now();
        }

        public String getName() { return name; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public int getMonitoringPort() { return monitoringPort; }
        public String getHealthPath() { return healthPath; }
        public boolean isHealthy() { return healthy; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public int getTotalChecks() { return totalChecks; }
        public long getUptime() { return System.currentTimeMillis() - startTime; }
        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
    }
}