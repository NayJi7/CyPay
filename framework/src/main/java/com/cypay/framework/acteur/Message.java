package com.cypay.framework.acteur;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message<T> {
    private final String id;
    private final String emetteur;
    private final T contenu;
    private final LocalDateTime timestamp;

    public Message(String emetteur, T contenu) {
        this.id = UUID.randomUUID().toString();
        this.emetteur = emetteur;
        this.contenu = contenu;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getEmetteur() { return emetteur; }
    public T getContenu() { return contenu; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
