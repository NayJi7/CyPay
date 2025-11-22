package com.cypay.logs.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acteur_logs")
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acteur_nom") // map la colonne réelle
    private String acteur;

    @Column(name = "niveau")
    private String niveau;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "log_time")
    private LocalDateTime logTime;

    // Getters et setters
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
}
