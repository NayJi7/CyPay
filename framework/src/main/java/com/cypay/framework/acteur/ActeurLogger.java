package com.cypay.framework.acteur;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActeurLogger {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String acteurNom;
    private final boolean logToDb;

    // ✅ Configuration de la BDD (peut être null si pas de logs en BDD)
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    /**
     * Constructeur simple : logs en console uniquement
     */
    public ActeurLogger(String acteurNom) {
        this(acteurNom, false, null, null, null);
    }

    /**
     * Constructeur avec choix console/BDD mais sans config BDD
     * (utilisera les valeurs par défaut si logToDb = true)
     */
    public ActeurLogger(String acteurNom, boolean logToDb) {
        this(acteurNom, logToDb, null, null, null);
    }

    /**
     * ✅ Constructeur complet : permet de configurer la BDD
     * @param acteurNom Le nom de l'acteur
     * @param logToDb true pour logger en BDD, false pour console uniquement
     * @param jdbcUrl L'URL JDBC de la base de données (peut être null)
     * @param dbUser Le nom d'utilisateur de la BDD (peut être null)
     * @param dbPassword Le mot de passe de la BDD (peut être null)
     */
    public ActeurLogger(String acteurNom, boolean logToDb, String jdbcUrl, String dbUser, String dbPassword) {
        this.acteurNom = acteurNom;
        this.logToDb = logToDb;
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void messageRecu(String emetteur, String typeMessage) {
        log("MSG_IN", String.format("Reçu de [%s] → %s", emetteur, typeMessage));
    }

    public void messageEnvoye(String destinataire, String typeMessage) {
        log("MSG_OUT", String.format("Envoyé vers [%s] → %s", destinataire, typeMessage));
    }

    public void httpRequest(String method, String url, int statusCode) {
        log("HTTP", String.format("%s %s → %d", method, url, statusCode));
    }

    public void erreur(String message, Exception e) {
        log("ERROR", message + " : " + (e != null ? e.getMessage() : ""));
    }

    private void log(String niveau, String message) {
        String formattedLog = String.format("[%s] [%s] [%s] %s",
                LocalDateTime.now().format(FORMATTER),
                niveau,
                acteurNom,
                message
        );

        System.out.println(formattedLog);

        if (logToDb && jdbcUrl != null && dbUser != null && dbPassword != null) {
            writeToDb(niveau, message);
        }
    }

    private void writeToDb(String niveau, String message) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO acteur_logs(acteur_nom, niveau, message) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, acteurNom);
                stmt.setString(2, niveau);
                stmt.setString(3, message);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'écriture du log dans la DB : " + e.getMessage());
        }
    }
}