package com.example.user.acteur;

import com.cypay.framework.acteur.Acteur;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SuperviseurActeur extends Acteur<Object> {

    private final Map<String, ActorStats> actorStats = new ConcurrentHashMap<>();
    private final Map<String, Acteur<?>> acteurs = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int MAX_RESTART_ATTEMPTS = 3;
    private static final long RESTART_WINDOW_MS = 60000;
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 10;

    public SuperviseurActeur() {
        super("SuperviseurActeur");
    }

    @Override
    public void demarrer() {
        super.demarrer();

        scheduler.scheduleAtFixedRate(
                this::performAutomaticHealthCheck,
                HEALTH_CHECK_INTERVAL_SECONDS,
                HEALTH_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log("✅ Vérifications automatiques activées (" + HEALTH_CHECK_INTERVAL_SECONDS + "s)");
    }

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
                    log("   Tentative de redémarrage...");
                    restartActor(name);
                    hasIssues = true;
                }
            }

            if (!hasIssues) {
                log("💚 Health check OK - Tous les acteurs actifs");
            }

        } catch (Exception e) {
            logErreur("❌ Erreur health check automatique", e);
        }
    }

    public void enregistrerActeur(String nom, Acteur<?> acteur) {
        acteurs.put(nom, acteur);
        actorStats.put(nom, new ActorStats(nom));

        acteur.setSupervisor(this);

        log("📋 Acteur enregistré : " + nom);
    }

    public void demarrerActeur(String nom) {
        Acteur<?> acteur = acteurs.get(nom);
        if (acteur == null) {
            log("❌ Acteur inconnu : " + nom);
            return;
        }

        try {
            log("🚀 Démarrage acteur : " + nom);
            acteur.demarrer();
            Thread.sleep(500);

            if (acteur.estActif()) {
                log("✅ " + nom + " démarré");
            } else {
                log("⚠️ " + nom + " démarré mais état incertain");
            }

        } catch (Exception e) {
            logErreur("❌ Échec démarrage " + nom, e);
        }
    }

    @Override
    protected void traiterMessage(Object message) {

        if (message instanceof ActorFailed m) {
            handleActorFailure(m);

        } else if (message instanceof HealthCheckRequest) {
            handleHealthCheck();

        } else if (message instanceof GetStatsRequest) {
            handleGetStats();

        } else if (message instanceof RestartActorRequest m) {
            handleRestartRequest(m);

        } else if (message instanceof ShutdownRequest) {
            handleShutdown();

        } else {
            log("⚠️ Message non géré : " + message.getClass().getSimpleName());
        }
    }

    private void handleActorFailure(ActorFailed failure) {
        String actorName = failure.actorName();
        Exception error = failure.error();
        long timestamp = failure.timestamp();

        ActorStats stats = actorStats.get(actorName);
        if (stats == null) {
            logErreur("❌ Acteur inconnu : " + actorName, error);
            return;
        }

        stats.recordFailure(timestamp, error);

        log("💥 DÉFAILLANCE détectée");
        log("   Acteur    : " + actorName);
        log("   Erreur    : " + error.getMessage());
        log("   Horodatage: " + LocalDateTime.now().format(formatter));
        log("   Tentatives: " + stats.getFailureCount() + "/" + MAX_RESTART_ATTEMPTS);

        if (shouldRestart(stats)) {
            log("🔄 Redémarrage automatique : " + actorName);
            restartActor(actorName);
        } else {
            log("🛑 Trop d'échecs - Acteur STOP : " + actorName);
            stats.markAsStopped();
        }
    }

    private boolean shouldRestart(ActorStats stats) {
        long now = System.currentTimeMillis();
        if (now - stats.getFirstFailureInWindow() > RESTART_WINDOW_MS) {
            stats.resetWindow();
        }
        return stats.getFailureCount() < MAX_RESTART_ATTEMPTS;
    }

    private void restartActor(String actorName) {
        Acteur<?> acteur = acteurs.get(actorName);
        if (acteur == null) {
            log("❌ Redémarrage impossible : acteur introuvable");
            return;
        }

        try {
            acteur.arreter();
            Thread.sleep(500);

            acteur.demarrer();

            actorStats.get(actorName).recordRestart();

            log("✅ Acteur redémarré : " + actorName);

        } catch (Exception e) {
            logErreur("❌ Erreur redémarrage : " + actorName, e);
        }
    }

    private void handleHealthCheck() {
        log("🏥 VÉRIFICATION DE SANTÉ (manuelle)");
        log("─────────────────────────────────────");

        boolean allHealthy = true;

        for (Map.Entry<String, Acteur<?>> entry : acteurs.entrySet()) {
            String name = entry.getKey();
            Acteur<?> acteur = entry.getValue();
            ActorStats stats = actorStats.get(name);

            boolean isActive = acteur.estActif();

            log(String.format("%-20s : %s | Échecs: %d | Restart: %d",
                    name,
                    isActive ? "✅ ACTIF" : "❌ MORT",
                    stats.getFailureCount(),
                    stats.getRestartCount()
            ));

            if (!isActive) allHealthy = false;
        }

        log("─────────────────────────────────────");
        log("Statut global : " + (allHealthy ? "🟢 OK" : "🔴 PROBLÈMES"));
    }

    private void handleGetStats() {
        log("📊 STATS DU SYSTÈME");
        log("════════════════════════════");

        for (ActorStats stats : actorStats.values()) {
            log("");
            log("Acteur : " + stats.getName());
            log("  État           : " + (stats.isStopped() ? "STOP" : "ACTIF"));
            log("  Échecs total   : " + stats.getTotalFailures());
            log("  Redémarrages   : " + stats.getRestartCount());
            log("  Uptime         : " + formatDuration(stats.getUptime()));
            log("  Dernier échec  : " +
                    (stats.getLastFailureTime() > 0
                            ? formatTimestamp(stats.getLastFailureTime())
                            : "Aucun"));
        }

        log("════════════════════════════");
    }

    private void handleRestartRequest(RestartActorRequest req) {
        log("🔄 Restart manuel : " + req.actorName());
        restartActor(req.actorName());
    }

    private void handleShutdown() {
        log("🛑 ARRÊT GLOBAL DU SYSTÈME...");

        scheduler.shutdown();

        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        for (Map.Entry<String, Acteur<?>> entry : acteurs.entrySet()) {
            try {
                log("   Arrêt : " + entry.getKey());
                entry.getValue().arreter();
                Thread.sleep(200);

            } catch (Exception e) {
                logErreur("❌ Erreur arrêt " + entry.getKey(), e);
            }
        }

        log("✅ ARRÊT TERMINÉ");
        this.arreter();
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private String formatTimestamp(long timestamp) {
        return LocalDateTime
                .ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
                .format(formatter);
    }

    // ======= MESSAGES =======
    public record ActorFailed(String actorName, Exception error, long timestamp) {}
    public record HealthCheckRequest() {}
    public record GetStatsRequest() {}
    public record RestartActorRequest(String actorName) {}
    public record ShutdownRequest() {}

    // ======= STATISTIQUES =======
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
            if (firstFailureInWindow == 0) firstFailureInWindow = timestamp;
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
        public long getLastFailureTime() { return lastFailureTime; }
        public boolean isStopped() { return stopped; }
        public long getUptime() { return System.currentTimeMillis() - creationTime; }
    }
}
