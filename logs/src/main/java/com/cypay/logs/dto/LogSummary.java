package com.cypay.logs.dto;

import java.time.LocalDateTime;

public class LogSummary {
    private Long id;
    private String acteur;
    private String niveau;
    private String message;
    private LocalDateTime logTime;

    public LogSummary(Long id, String acteur, String niveau, String message, LocalDateTime logTime) {
        this.id = id;
        this.acteur = acteur;
        this.niveau = niveau;
        this.message = message;
        this.logTime = logTime;
    }

    // getters et setters
    public Long getId() { return id; }
    public String getActeur() { return acteur; }
    public String getNiveau() { return niveau; }
    public String getMessage() { return message; }
    public LocalDateTime getLogTime() { return logTime; }
}
