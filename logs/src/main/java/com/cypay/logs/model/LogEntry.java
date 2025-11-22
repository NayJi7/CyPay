package com.cypay.logs.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modèle de données pour une entrée de log
 */
public class LogEntry {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String acteur;
    private String niveau;
    private String message;
    private LocalDateTime logTime;

    public LogEntry() {
    }

    public LogEntry(Long id, String acteur, String niveau, String message, LocalDateTime logTime) {
        this.id = id;
        this.acteur = acteur;
        this.niveau = niveau;
        this.message = message;
        this.logTime = logTime;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActeur() {
        return acteur;
    }

    public void setActeur(String acteur) {
        this.acteur = acteur;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(LocalDateTime logTime) {
        this.logTime = logTime;
    }

    public String getFormattedLogTime() {
        return logTime != null ? logTime.format(FORMATTER) : "N/A";
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] %s",
                getFormattedLogTime(), niveau, acteur, message);
    }
}