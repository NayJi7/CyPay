package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;

import java.util.*;

/**
 * 🛡️ Superviseur qui surveille et redémarre les acteurs
 */
public class SupervisorActeur extends Acteur<Object> {

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    // Acteurs supervisés
    private DatabaseActeur databaseActeur;
    private StatsActeur statsActeur;
    private LogHttpActeur logHttpActeur;

    // Stratégie de supervision
    private final SupervisionStrategy strategy;

    // Compteurs de redémarrages
    private final Map<String, Integer> restartCounts = new HashMap<>();
    private final Map<String, Long> lastRestartTime = new HashMap<>();
    private final int maxRestarts = 3;
    private final long resetInterval = 60000; // 1 minute

    public SupervisorActeur(String jdbcUrl, String dbUser, String dbPassword,
                            SupervisionStrategy strategy) {
        super("SupervisorActeur");
        this.logger = new ActeurLogger("SupervisorActeur", true,
                jdbcUrl, dbUser, dbPassword);
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.strategy = strategy;
    }

    /**
     * Initialise et démarre tous les acteurs supervisés
     */
    public void initializeChildren() {
        getLogger().info("🎬 Initialisation des acteurs supervisés");

        // Créer les acteurs
        databaseActeur = new DatabaseActeur(jdbcUrl, dbUser, dbPassword);
        statsActeur = new StatsActeur(jdbcUrl, dbUser, dbPassword);
        logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);

        // Configurer la supervision
        databaseActeur.setSupervisor(this);
        statsActeur.setSupervisor(this);
        logHttpActeur.setSupervisor(this);

        // Démarrer les acteurs
        databaseActeur.demarrer();
        statsActeur.demarrer();
        logHttpActeur.demarrer();

        getLogger().info("✅ Tous les acteurs supervisés démarrés");
    }

    @Override
    protected void traiterMessage(Object message) {
        if (message instanceof Messages.ActorFailed failure) {
            handleActorFailure(failure);

        } else if (message instanceof Messages.HealthCheck check) {
            handleHealthCheck();

        } else {
            getLogger().info("❌ Message non reconnu : " +
                    message.getClass().getSimpleName());
        }
    }

    /**
     * Gère la défaillance d'un acteur
     */
    private void handleActorFailure(Messages.ActorFailed failure) {
        String actorName = failure.actorName();
        Throwable error = failure.error();

        // Convertir Throwable en Exception pour le logger
        Exception ex = (error instanceof Exception) ? (Exception) error : new Exception(error);
        getLogger().erreur("💥 Acteur défaillant : " + actorName, ex);

        // Vérifier et réinitialiser le compteur si nécessaire
        long now = System.currentTimeMillis();
        Long lastRestart = lastRestartTime.get(actorName);

        if (lastRestart != null && (now - lastRestart) > resetInterval) {
            // Plus d'une minute s'est écoulée, réinitialiser le compteur
            restartCounts.put(actorName, 0);
            getLogger().info("🔄 Compteur de redémarrages réinitialisé pour " + actorName);
        }

        // Compter les redémarrages
        int count = restartCounts.getOrDefault(actorName, 0) + 1;
        restartCounts.put(actorName, count);
        lastRestartTime.put(actorName, now);

        // Vérifier si on dépasse la limite
        if (count > maxRestarts) {
            getLogger().info("🚨 ALERTE : " + actorName +
                    " a crashé " + count + " fois en moins d'une minute. ARRÊT DÉFINITIF.");

            if (strategy == SupervisionStrategy.ESCALATE) {
                getLogger().info("🛑 Arrêt de tous les acteurs");
                stopAllActors();
                System.exit(1);
            }
            return;
        }

        // Appliquer la stratégie de supervision
        switch (strategy) {
            case RESTART -> restartActor(actorName);
            case RESUME -> getLogger().info("▶️ Reprise de : " + actorName);
            case STOP -> stopActor(actorName);
            case ESCALATE -> escalateFailure(actorName, error);
        }
    }

    /**
     * Redémarre un acteur spécifique
     */
    private void restartActor(String actorName) {
        getLogger().info("🔄 Redémarrage de : " + actorName);

        try {
            switch (actorName) {
                case "DatabaseActeur" -> {
                    if (databaseActeur != null) {
                        databaseActeur.arreter();
                    }
                    Thread.sleep(100); // Petit délai

                    databaseActeur = new DatabaseActeur(jdbcUrl, dbUser, dbPassword);
                    databaseActeur.setSupervisor(this);
                    databaseActeur.demarrer();

                    // Recréer LogHttpActeur qui dépend de DatabaseActeur
                    if (logHttpActeur != null) {
                        logHttpActeur.arreter();
                        Thread.sleep(100);
                    }
                    logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
                    logHttpActeur.setSupervisor(this);
                    logHttpActeur.demarrer();
                }
                case "StatsActeur" -> {
                    if (statsActeur != null) {
                        statsActeur.arreter();
                    }
                    Thread.sleep(100);

                    statsActeur = new StatsActeur(jdbcUrl, dbUser, dbPassword);
                    statsActeur.setSupervisor(this);
                    statsActeur.demarrer();

                    // Recréer LogHttpActeur
                    if (logHttpActeur != null) {
                        logHttpActeur.arreter();
                        Thread.sleep(100);
                    }
                    logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
                    logHttpActeur.setSupervisor(this);
                    logHttpActeur.demarrer();
                }
                case "LogHttpActeur" -> {
                    if (logHttpActeur != null) {
                        logHttpActeur.arreter();
                    }
                    Thread.sleep(100);

                    logHttpActeur = new LogHttpActeur(databaseActeur, statsActeur);
                    logHttpActeur.setSupervisor(this);
                    logHttpActeur.demarrer();
                }
            }

            getLogger().info("✅ Acteur redémarré avec succès : " + actorName);

        } catch (Exception e) {
            getLogger().erreur("❌ Échec du redémarrage de " + actorName, e);
        }
    }

    /**
     * Arrête l'acteur définitivement
     */
    private void stopActor(String actorName) {
        getLogger().info("🛑 Arrêt définitif de : " + actorName);

        switch (actorName) {
            case "DatabaseActeur" -> {
                if (databaseActeur != null) databaseActeur.arreter();
            }
            case "StatsActeur" -> {
                if (statsActeur != null) statsActeur.arreter();
            }
            case "LogHttpActeur" -> {
                if (logHttpActeur != null) logHttpActeur.arreter();
            }
        }
    }

    /**
     * Remonte l'erreur (arrête tout)
     */
    private void escalateFailure(String actorName, Throwable error) {
        Exception ex = (error instanceof Exception) ? (Exception) error : new Exception(error);
        getLogger().erreur("🚨 ESCALADE : Erreur critique de " + actorName, ex);
        stopAllActors();
        System.exit(1);
    }

    /**
     * Arrête tous les acteurs
     */
    private void stopAllActors() {
        getLogger().info("🛑 Arrêt de tous les acteurs");

        if (logHttpActeur != null) logHttpActeur.arreter();
        if (databaseActeur != null) databaseActeur.arreter();
        if (statsActeur != null) statsActeur.arreter();
    }

    /**
     * Vérifie la santé des acteurs
     */
    private void handleHealthCheck() {
        boolean allHealthy = true;

        if (databaseActeur == null || !databaseActeur.estActif()) {
            getLogger().info("⚠️ DatabaseActeur n'est pas actif");
            allHealthy = false;
        }

        if (statsActeur == null || !statsActeur.estActif()) {
            getLogger().info("⚠️ StatsActeur n'est pas actif");
            allHealthy = false;
        }

        if (logHttpActeur == null || !logHttpActeur.estActif()) {
            getLogger().info("⚠️ LogHttpActeur n'est pas actif");
            allHealthy = false;
        }

        getLogger().info("💊 Health check : " + (allHealthy ? "✅ OK" : "⚠️ DEGRADED"));
    }

    /**
     * Déclenche un health check
     */
    public void triggerHealthCheck() {
        envoyerVers(this, new Messages.HealthCheck("manual-check"));
    }

    /**
     * Getters pour accéder aux acteurs
     */
    public LogHttpActeur getLogHttpActeur() {
        return logHttpActeur;
    }

    public DatabaseActeur getDatabaseActeur() {
        return databaseActeur;
    }

    public StatsActeur getStatsActeur() {
        return statsActeur;
    }

    /**
     * Stratégies de supervision
     */
    public enum SupervisionStrategy {
        RESTART,   // Redémarrer l'acteur
        RESUME,    // Continuer avec le prochain message
        STOP,      // Arrêter l'acteur
        ESCALATE   // Remonter l'erreur (arrêter tout)
    }
}