package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ✅ Supervisor pour le microservice Logs
 * Supervise : DatabaseActeur, StatsActeur, LogHttpActeur, LogsMonitoringActeur
 * - Health checks automatiques toutes les 10s
 * - Redémarrage automatique en cas d'erreur
 * - Max 3 tentatives par minute
 */
public class SupervisorActeur extends Acteur<Object> {

    // ========== GESTION DES ACTEURS ==========

    private final Map<String, ActorStats> actorStats = new ConcurrentHashMap<>();
    private final Map<String, Acteur<?>> acteurs = new ConcurrentHashMap<>();

    // Références spécifiques aux acteurs
    private DatabaseActeur databaseActeur;
    private StatsActeur statsActeur;
    private LogHttpActeur logHttpActeur;

    // ========== CONFIGURATION ==========

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int MAX_RESTART_ATTEMPTS = 3;
    private static final long RESTART_WINDOW_MS = 60000; // 1 minute
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 10;

    // ========== CONSTRUCTEUR ==========

    public SupervisorActeur(String jdbcUrl, String dbUser, String dbPassword) {
        super("LogsSupervisorActeur");
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    // ========== DÉMARRAGE ==========

    @Override
    public void demarrer() {
        super.demarrer();

        // Lancer les health checks automatiques
        scheduler.scheduleAtFixedRate(
                this::performAutomaticHealthCheck,
                HEALTH_CHECK_INTERVAL_SECONDS,
                HEALTH_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log("✅ Vérifications de santé automatiques activées (toutes les " + HEALTH_CHECK_INTERVAL_SECONDS + "s)");
    }

    // ========== INITIALISATION DES ACTEURS ENFANTS ==========

    /**
     * Initialise et démarre tous les acteurs supervisés
     */
    public void initializeChildren() {
        log("📋 Initialisation des acteurs enfants...");

        // 1. DatabaseActeur
        databaseActeur = new DatabaseActeur(jdbcUrl, dbUser, dbPassword);
        enregistrerActeur("DatabaseActeur", databaseActeur);
        demarrerActeur("DatabaseActeur");

        // 2. StatsActeur
        statsActeur = new StatsActeur(jdbcUrl, dbUser, dbPassword);
        enregistrerActeur("StatsActeur", statsActeur);
        demarrerActeur("StatsActeur");

        // 3. LogHttpActeur
        logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
        enregistrerActeur("LogHttpActeur", logHttpActeur);
        demarrerActeur("LogHttpActeur");

        log("✅ Tous les acteurs enfants démarrés");
    }

    /**
     * Enregistre un acteur à superviser
     */
    public void enregistrerActeur(String nom, Acteur<?> acteur) {
        acteurs.put(nom, acteur);
        actorStats.put(nom, new ActorStats(nom));
        acteur.setSupervisor(this);
        log("📋 Acteur enregistré : " + nom);
    }

    /**
     * Démarre un acteur supervisé
     */
    public void demarrerActeur(String nom) {
        Acteur<?> acteur = acteurs.get(nom);
        if (acteur == null) {
            log("❌ Acteur inconnu : " + nom);
            return;
        }

        try {
            log("🚀 Démarrage de l'acteur : " + nom);
            acteur.demarrer();
            Thread.sleep(500);

            if (acteur.estActif()) {
                log("✅ " + nom + " démarré avec succès");
            } else {
                log("⚠️ " + nom + " démarré mais état incertain");
            }
        } catch (Exception e) {
            logErreur("❌ Échec du démarrage de " + nom, e);
        }
    }

    // ========== HEALTH CHECKS AUTOMATIQUES ==========

    /**
     * Vérifie automatiquement l'état de tous les acteurs
     */
    private void performAutomaticHealthCheck() {
        try {
            boolean hasIssues = false;

            for (Map.Entry<String, Acteur<?>> entry : acteurs.entrySet()) {
                String name = entry.getKey();
                Acteur<?> acteur = entry.getValue();
                ActorStats stats = actorStats.get(name);

                boolean isActive = acteur.estActif();

                if (!isActive && !stats.isStopped()) {
                    log("⚠️ ALERTE : " + name + " est inactif !");
                    log("   Tentative de redémarrage automatique...");
                    restartActor(name);
                    hasIssues = true;
                }
            }

            if (!hasIssues) {
                log("💚 Health check OK - Tous les acteurs sont actifs");
            }

        } catch (Exception e) {
            logErreur("❌ Erreur lors du health check automatique", e);
        }
    }

    // ========== TRAITEMENT DES MESSAGES ==========

    @Override
    protected void traiterMessage(Object message) {

        if (message instanceof Messages.ActorFailed) {
            handleActorFailure((Messages.ActorFailed) message);

        } else if (message instanceof HealthCheckRequest) {
            handleHealthCheck();

        } else if (message instanceof GetStatsRequest) {
            handleGetStats();

        } else if (message instanceof RestartActorRequest) {
            handleRestartRequest((RestartActorRequest) message);

        } else if (message instanceof ShutdownRequest) {
            handleShutdown();

        } else {
            log("⚠️ Message non géré : " + message.getClass().getSimpleName());
        }
    }

    // ========== GESTION DES DÉFAILLANCES ==========

    /**
     * Gère la défaillance d'un acteur
     */
    private void handleActorFailure(Messages.ActorFailed failure) {
        String actorName = failure.actorName();
        Throwable error = failure.error();
        long timestamp = failure.timestamp();

        ActorStats stats = actorStats.get(actorName);
        if (stats == null) {
            Exception ex = (error instanceof Exception) ? (Exception) error : new Exception(error);
            logErreur("❌ Acteur inconnu : " + actorName, ex);
            return;
        }

        // Enregistrer l'échec
        Exception ex = (error instanceof Exception) ? (Exception) error : new Exception(error);
        stats.recordFailure(timestamp, ex);

        log("💥 DÉFAILLANCE DÉTECTÉE");
        log("   Acteur    : " + actorName);
        log("   Erreur    : " + error.getMessage());
        log("   Timestamp : " + LocalDateTime.now().format(formatter));
        log("   Tentatives: " + stats.getFailureCount() + "/" + MAX_RESTART_ATTEMPTS);

        // Décider de la stratégie
        if (shouldRestart(stats)) {
            log("🔄 Redémarrage automatique de l'acteur : " + actorName);
            restartActor(actorName);
        } else {
            log("🛑 TROP DE DÉFAILLANCES - Acteur arrêté : " + actorName);
            log("   Action requise : Investigation manuelle");
            stats.markAsStopped();
        }
    }

    /**
     * Détermine si l'acteur doit être redémarré
     */
    private boolean shouldRestart(ActorStats stats) {
        long now = System.currentTimeMillis();
        if (now - stats.getFirstFailureInWindow() > RESTART_WINDOW_MS) {
            stats.resetWindow();
        }
        return stats.getFailureCount() < MAX_RESTART_ATTEMPTS;
    }

    /**
     * Redémarre un acteur
     */
    private void restartActor(String actorName) {
        try {
            Acteur<?> acteur = acteurs.get(actorName);
            if (acteur == null) {
                log("❌ Impossible de redémarrer : acteur non trouvé");
                return;
            }

            // Arrêter l'acteur
            acteur.arreter();
            Thread.sleep(500);

            // Recréer et redémarrer selon le type
            switch (actorName) {
                case "DatabaseActeur" -> {
                    databaseActeur = new DatabaseActeur(jdbcUrl, dbUser, dbPassword);
                    acteurs.put("DatabaseActeur", databaseActeur);
                    databaseActeur.setSupervisor(this);
                    databaseActeur.demarrer();

                    // Recréer LogHttpActeur qui dépend de DatabaseActeur
                    if (logHttpActeur != null) {
                        logHttpActeur.arreter();
                        Thread.sleep(100);
                    }
                    logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
                    acteurs.put("LogHttpActeur", logHttpActeur);
                    logHttpActeur.setSupervisor(this);
                    logHttpActeur.demarrer();
                }
                case "StatsActeur" -> {
                    statsActeur = new StatsActeur(jdbcUrl, dbUser, dbPassword);
                    acteurs.put("StatsActeur", statsActeur);
                    statsActeur.setSupervisor(this);
                    statsActeur.demarrer();

                    // Recréer LogHttpActeur qui dépend de StatsActeur
                    if (logHttpActeur != null) {
                        logHttpActeur.arreter();
                        Thread.sleep(100);
                    }
                    logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
                    acteurs.put("LogHttpActeur", logHttpActeur);
                    logHttpActeur.setSupervisor(this);
                    logHttpActeur.demarrer();
                }
                case "LogHttpActeur" -> {
                    logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
                    acteurs.put("LogHttpActeur", logHttpActeur);
                    logHttpActeur.setSupervisor(this);
                    logHttpActeur.demarrer();
                }
                default -> {
                    acteur.demarrer();
                }
            }

            ActorStats stats = actorStats.get(actorName);
            stats.recordRestart();

            log("✅ Acteur redémarré avec succès : " + actorName);
            log("   Redémarrages totaux : " + stats.getRestartCount());

        } catch (Exception e) {
            logErreur("❌ Échec du redémarrage de l'acteur : " + actorName, e);
        }
    }

    // ========== HEALTH CHECK MANUEL ==========

    /**
     * Health check manuel déclenché via l'API
     */
    private void handleHealthCheck() {
        log("🏥 VÉRIFICATION DE SANTÉ DU SYSTÈME (MANUELLE)");
        log("─────────────────────────────────────────────────");

        boolean allHealthy = true;

        for (Map.Entry<String, Acteur<?>> entry : acteurs.entrySet()) {
            String name = entry.getKey();
            Acteur<?> acteur = entry.getValue();
            ActorStats stats = actorStats.get(name);

            boolean isActive = acteur.estActif();
            String status = isActive ? "✅ ACTIF" : "❌ ARRÊTÉ";

            log(String.format("%-25s : %s | Échecs: %d | Redémarrages: %d",
                    name, status, stats.getFailureCount(), stats.getRestartCount()));

            if (!isActive) allHealthy = false;
        }

        log("─────────────────────────────────────────────────");
        log("Statut global : " + (allHealthy ? "✅ SYSTÈME SAIN" : "⚠️ PROBLÈMES DÉTECTÉS"));
    }

    // ========== STATISTIQUES ==========

    /**
     * Affiche les statistiques détaillées
     */
    private void handleGetStats() {
        log("📊 STATISTIQUES DU SYSTÈME");
        log("═════════════════════════════════════════════════");

        for (Map.Entry<String, ActorStats> entry : actorStats.entrySet()) {
            ActorStats stats = entry.getValue();
            log("");
            log("Acteur : " + stats.getName());
            log("  État           : " + (stats.isStopped() ? "ARRÊTÉ" : "ACTIF"));
            log("  Échecs         : " + stats.getTotalFailures());
            log("  Redémarrages   : " + stats.getRestartCount());
            log("  Uptime         : " + formatDuration(stats.getUptime()));
        }

        log("═════════════════════════════════════════════════");
    }

    // ========== REDÉMARRAGE MANUEL ==========

    /**
     * Redémarrage manuel d'un acteur
     */
    private void handleRestartRequest(RestartActorRequest req) {
        log("🔄 Demande de redémarrage manuel : " + req.actorName());
        restartActor(req.actorName());
    }

    // ========== ARRÊT DU SYSTÈME ==========

    /**
     * Arrêt propre de tous les acteurs
     */
    private void handleShutdown() {
        log("🛑 ARRÊT DU SYSTÈME EN COURS...");

        // Arrêter le scheduler
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        // Arrêter tous les acteurs
        for (Map.Entry<String, Acteur<?>> entry : acteurs.entrySet()) {
            String name = entry.getKey();
            Acteur<?> acteur = entry.getValue();

            try {
                log("   Arrêt de " + name + "...");
                acteur.arreter();
                Thread.sleep(200);
                log("   ✅ " + name + " arrêté");
            } catch (Exception e) {
                logErreur("   ❌ Erreur arrêt " + name, e);
            }
        }

        log("✅ ARRÊT DU SYSTÈME TERMINÉ");
        this.arreter();
    }

    // ========== GETTERS ==========

    public LogHttpActeur getLogHttpActeur() {
        return logHttpActeur;
    }

    public DatabaseActeur getDatabaseActeur() {
        return databaseActeur;
    }

    public StatsActeur getStatsActeur() {
        return statsActeur;
    }

    // ========== UTILITAIRES ==========

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    // ========== MESSAGES ==========

    public record HealthCheckRequest() {}
    public record GetStatsRequest() {}
    public record RestartActorRequest(String actorName) {}
    public record ShutdownRequest() {}

    // ========== STATISTIQUES PAR ACTEUR ==========

    private static class ActorStats {
        private final String name;
        private final long creationTime;
        private int failureCount;
        private int totalFailures;
        private int restartCount;
        private long firstFailureInWindow;
        private long lastFailureTime;
        private boolean stopped;

        public ActorStats(String name) {
            this.name = name;
            this.creationTime = System.currentTimeMillis();
        }

        public void recordFailure(long timestamp, Exception error) {
            if (firstFailureInWindow == 0) {
                firstFailureInWindow = timestamp;
            }
            failureCount++;
            totalFailures++;
            lastFailureTime = timestamp;
        }

        public void recordRestart() {
            restartCount++;
            stopped = false;
        }

        public void resetWindow() {
            failureCount = 0;
            firstFailureInWindow = 0;
        }

        public void markAsStopped() {
            stopped = true;
        }

        public String getName() { return name; }
        public int getFailureCount() { return failureCount; }
        public int getTotalFailures() { return totalFailures; }
        public int getRestartCount() { return restartCount; }
        public long getFirstFailureInWindow() { return firstFailureInWindow; }
        public boolean isStopped() { return stopped; }
        public long getUptime() { return System.currentTimeMillis() - creationTime; }
    }
}