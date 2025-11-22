package com.cypay.logs.acteur;

import com.cypay.framework.acteur.Acteur;
import com.cypay.framework.acteur.ActeurLogger;
import com.cypay.logs.model.LogEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Acteur dédié aux opérations base de données
 * Traite toutes les requêtes SQL dans son propre thread
 */
public class DatabaseActeur extends Acteur<Object> {

    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseActeur(String jdbcUrl, String dbUser, String dbPassword) {
        super("DatabaseActeur");
        this.logger = new ActeurLogger(
                "DatabaseActeur",
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
        if (message instanceof Messages.GetAllLogsQuery query) {
            handleGetAllLogs(query);

        } else if (message instanceof Messages.GetLogsByActorQuery query) {
            handleGetLogsByActor(query);

        } else if (message instanceof Messages.GetLogsByLevelQuery query) {
            handleGetLogsByLevel(query);

        } else if (message instanceof Messages.DeleteAllLogsCommand command) {
            handleDeleteAllLogs(command);

        } else {
            getLogger().info("❌ Message non reconnu : " + message.getClass().getSimpleName());
        }
    }

    /**
     * Récupère tous les logs
     */
    private void handleGetAllLogs(Messages.GetAllLogsQuery query) {
        getLogger().info("🔍 Récupération de tous les logs (limit: " + query.limit() + ")");

        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM acteur_logs ORDER BY log_time DESC LIMIT ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, query.limit());

                ResultSet rs = stmt.executeQuery();
                List<LogEntry> logs = mapResultSet(rs);

                getLogger().info("✅ " + logs.size() + " logs récupérés");

                // Réponse ASYNCHRONE vers l'acteur HTTP
                envoyerVers(query.replyTo(), new Messages.LogsResponse(
                        query.requestId(),
                        query.exchange(),
                        logs,
                        true,
                        null
                ));
            }

        } catch (SQLException e) {
            getLogger().erreur("❌ Erreur SQL getAllLogs", e);

            envoyerVers(query.replyTo(), new Messages.LogsResponse(
                    query.requestId(),
                    query.exchange(),
                    List.of(),
                    false,
                    "Erreur base de données: " + e.getMessage()
            ));
        }
    }

    /**
     * Récupère les logs d'un acteur spécifique
     */
    private void handleGetLogsByActor(Messages.GetLogsByActorQuery query) {
        getLogger().info("🔍 Récupération logs pour acteur: " + query.acteur());

        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM acteur_logs WHERE acteur_nom = ? " +
                    "ORDER BY log_time DESC LIMIT ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, query.acteur());
                stmt.setInt(2, query.limit());

                ResultSet rs = stmt.executeQuery();
                List<LogEntry> logs = mapResultSet(rs);

                getLogger().info("✅ " + logs.size() + " logs récupérés pour " + query.acteur());

                envoyerVers(query.replyTo(), new Messages.LogsResponse(
                        query.requestId(),
                        query.exchange(),
                        logs,
                        true,
                        null
                ));
            }

        } catch (SQLException e) {
            getLogger().erreur("❌ Erreur SQL getLogsByActor", e);

            envoyerVers(query.replyTo(), new Messages.LogsResponse(
                    query.requestId(),
                    query.exchange(),
                    List.of(),
                    false,
                    "Erreur base de données: " + e.getMessage()
            ));
        }
    }

    /**
     * Récupère les logs par niveau
     */
    private void handleGetLogsByLevel(Messages.GetLogsByLevelQuery query) {
        getLogger().info("🔍 Récupération logs niveau: " + query.niveau());

        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM acteur_logs WHERE niveau = ? " +
                    "ORDER BY log_time DESC LIMIT ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, query.niveau());
                stmt.setInt(2, query.limit());

                ResultSet rs = stmt.executeQuery();
                List<LogEntry> logs = mapResultSet(rs);

                getLogger().info("✅ " + logs.size() + " logs niveau " + query.niveau());

                envoyerVers(query.replyTo(), new Messages.LogsResponse(
                        query.requestId(),
                        query.exchange(),
                        logs,
                        true,
                        null
                ));
            }

        } catch (SQLException e) {
            getLogger().erreur("❌ Erreur SQL getLogsByLevel", e);

            envoyerVers(query.replyTo(), new Messages.LogsResponse(
                    query.requestId(),
                    query.exchange(),
                    List.of(),
                    false,
                    "Erreur base de données: " + e.getMessage()
            ));
        }
    }

    /**
     * Supprime tous les logs
     */
    private void handleDeleteAllLogs(Messages.DeleteAllLogsCommand command) {
        getLogger().info("🗑️ Suppression de tous les logs");

        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM acteur_logs";

            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(sql);

                getLogger().info("✅ " + deleted + " logs supprimés");

                envoyerVers(command.replyTo(), new Messages.DeleteResponse(
                        command.requestId(),
                        command.exchange(),
                        deleted,
                        true,
                        null
                ));
            }

        } catch (SQLException e) {
            getLogger().erreur("❌ Erreur SQL deleteAllLogs", e);

            envoyerVers(command.replyTo(), new Messages.DeleteResponse(
                    command.requestId(),
                    command.exchange(),
                    0,
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

    /**
     * Mappe un ResultSet vers une liste de LogEntry
     */
    private List<LogEntry> mapResultSet(ResultSet rs) throws SQLException {
        List<LogEntry> logs = new ArrayList<>();

        while (rs.next()) {
            LogEntry log = new LogEntry(
                    rs.getLong("id"),
                    rs.getString("acteur_nom"),
                    rs.getString("niveau"),
                    rs.getString("message"),
                    rs.getTimestamp("log_time").toLocalDateTime()
            );
            logs.add(log);
        }

        return logs;
    }
}