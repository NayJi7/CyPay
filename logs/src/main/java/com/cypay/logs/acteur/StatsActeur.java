package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;

import java.sql.*;

/**
 * ✅ Acteur spécialisé dans le calcul des statistiques
 */
public class StatsActeur extends Acteur<Object> {

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    public StatsActeur(String jdbcUrl, String dbUser, String dbPassword) {
        super("StatsActeur");
        this.logger = new ActeurLogger(
                "StatsActeur",
                true,
                jdbcUrl,
                dbUser,
                dbPassword
        );
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    @Override
    protected void traiterMessage(Object message) {
        if (message instanceof Messages.GetStatsQuery query) {
            handleGetStats(query);
        } else {
            getLogger().info("❌ Message non reconnu : " + message.getClass().getSimpleName());
        }
    }

    /**
     * Calcule les statistiques globales
     */
    private void handleGetStats(Messages.GetStatsQuery query) {
        getLogger().info("📊 Calcul des statistiques");

        try (Connection conn = getConnection()) {

            // Requête SQL pour les statistiques
            String sql = """
                SELECT 
                    COUNT(*) as total,
                    COUNT(CASE WHEN niveau = 'ERROR' THEN 1 END) as errors,
                    COUNT(CASE WHEN niveau = 'INFO' THEN 1 END) as infos,
                    COUNT(CASE WHEN niveau = 'WARNING' THEN 1 END) as warnings,
                    COUNT(CASE WHEN niveau = 'HTTP' THEN 1 END) as http_requests,
                    COUNT(CASE WHEN niveau = 'MSG_IN' THEN 1 END) as msg_in,
                    COUNT(CASE WHEN niveau = 'MSG_OUT' THEN 1 END) as msg_out,
                    COUNT(DISTINCT acteur_nom) as distinct_actors,
                    MIN(log_time) as first_log,
                    MAX(log_time) as last_log
                FROM acteur_logs
            """;

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    Messages.LogStats stats = new Messages.LogStats(
                            rs.getLong("total"),
                            rs.getLong("errors"),
                            rs.getLong("infos"),
                            rs.getLong("warnings"),
                            rs.getLong("http_requests"),
                            rs.getLong("msg_in"),
                            rs.getLong("msg_out"),
                            rs.getInt("distinct_actors"),
                            rs.getTimestamp("first_log") != null ?
                                    rs.getTimestamp("first_log").toString() : "N/A",
                            rs.getTimestamp("last_log") != null ?
                                    rs.getTimestamp("last_log").toString() : "N/A"
                    );

                    getLogger().info("✅ Statistiques calculées : " + stats.total() + " logs");

                    envoyerVers(query.replyTo(), new Messages.StatsResponse(
                            query.requestId(),
                            query.exchange(),
                            stats,
                            true,
                            null
                    ));

                } else {
                    getLogger().info("⚠️ Aucune statistique disponible");

                    envoyerVers(query.replyTo(), new Messages.StatsResponse(
                            query.requestId(),
                            query.exchange(),
                            null,
                            false,
                            "Aucune donnée disponible"
                    ));
                }
            }

        } catch (SQLException e) {
            getLogger().erreur("❌ Erreur SQL getStats", e);

            envoyerVers(query.replyTo(), new Messages.StatsResponse(
                    query.requestId(),
                    query.exchange(),
                    null,
                    false,
                    "Erreur base de données: " + e.getMessage()
            ));
        }
    }

    /**
     * Crée une connexion à la base de données
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }
}