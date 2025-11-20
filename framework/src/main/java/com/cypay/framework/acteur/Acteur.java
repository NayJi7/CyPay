package com.cypay.framework.acteur;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class Acteur<T> implements Runnable {

    private final String nom;
    private final BlockingQueue<Message<T>> mailbox;
    private volatile boolean running;
    private final ActeurLogger logger;
    private final ActeurHttpClient httpClient;
    private Thread thread;

    public Acteur(String nom) {
        this.nom = nom;
        this.mailbox = new LinkedBlockingQueue<>();
        this.running = true;
        this.logger = new ActeurLogger(nom);
        this.httpClient = new ActeurHttpClient(logger);
    }

    public void demarrer() {
        this.thread = new Thread(this, nom + "-Thread");
        this.thread.start();
        logger.info("Acteur démarré");
    }

    /**
     * Envoie un message à cet acteur
     */
    public void envoyer(Message<T> message) {
        try {
            mailbox.put(message);
            logger.messageRecu(message.getEmetteur(), message.getContenu().getClass().getSimpleName());
        } catch (InterruptedException e) {
            logger.erreur("Erreur lors de l'envoi du message", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Envoie un message à un autre acteur
     */
    protected void envoyerVers(Acteur<?> destinataire, Object contenu) {
        Message<?> message = new Message<>(this.nom, contenu);
        logger.messageEnvoye(destinataire.getNom(), contenu.getClass().getSimpleName());
        destinataire.envoyer((Message) message);
    }

    /**
     * Fait une requête HTTP GET
     */
    protected HttpResponse get(String url) {
        return httpClient.get(url);
    }

    /**
     * Fait une requête HTTP POST
     */
    protected HttpResponse post(String url, String jsonBody) {
        return httpClient.post(url, jsonBody);
    }

    /**
     * Fait une requête HTTP PUT
     */
    protected HttpResponse put(String url, String jsonBody) {
        return httpClient.put(url, jsonBody);
    }

    /**
     * Fait une requête HTTP DELETE
     */
    protected HttpResponse delete(String url) {
        return httpClient.delete(url);
    }

    /**
     * Fait une requête HTTP personnalisée
     */
    protected HttpResponse request(CustomHttpRequest request) {
        return httpClient.execute(request);
    }

    /**
     * Log une information
     */
    protected void log(String message) {
        logger.info(message);
    }

    /**
     * Log une erreur
     */
    protected void logErreur(String message, Exception e) {
        logger.erreur(message, e);
    }

    /**
     * Boucle principale de l'acteur
     */
    @Override
    public void run() {
        while (running) {
            try {
                Message<T> message = mailbox.take();
                traiterMessage(message.getContenu());
            } catch (InterruptedException e) {
                logger.erreur("Thread interrompu", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.erreur("Erreur lors du traitement du message", e);
            }
        }
        logger.info("Acteur arrêté");
    }

    /**
     * Méthode abstraite à implémenter par les classes filles
     */
    protected abstract void traiterMessage(T message);

    /**
     * Arrête l'acteur
     */
    public void arreter() {
        this.running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public String getNom() {
        return nom;
    }

    protected ActeurLogger getLogger() {
        return logger;
    }
}
