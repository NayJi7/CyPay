package com.cypay.framework.acteur;

import com.cypay.framework.http.HttpResponse;
import com.cypay.framework.http.HttpMethode;
import com.cypay.framework.http.HttpReceiver;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class Acteur<T> implements Runnable {

    private final String nom;
    private final BlockingQueue<Message<T>> mailbox;
    private volatile boolean running;
    protected ActeurLogger logger;
    private final ActeurHttpClient httpClient;
    private Thread thread;
    private HttpReceiver httpReceiver;

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

    public void envoyer(Message<T> message) {
        try {
            mailbox.put(message);
            logger.messageRecu(message.getEmetteur(), message.getContenu().getClass().getSimpleName());
        } catch (InterruptedException e) {
            logger.erreur("Erreur lors de l'envoi du message", e);
            Thread.currentThread().interrupt();
        }
    }

    public void envoyerObjet(Object contenu) {
        Message<?> message = new Message<>("SYSTEM", contenu);
        try {
            mailbox.put((Message<T>) message);
            logger.messageRecu("SYSTEM", contenu.getClass().getSimpleName());
        } catch (InterruptedException e) {
            logger.erreur("Erreur lors de l'envoi du message", e);
            Thread.currentThread().interrupt();
        }
    }

    protected void envoyerVers(Acteur<?> destinataire, Object contenu) {
        Message<?> message = new Message<>(this.nom, contenu);
        logger.messageEnvoye(destinataire.getNom(), contenu.getClass().getSimpleName());
        destinataire.envoyer((Message) message);
    }


    protected HttpResponse get(String url) {
        return httpClient.get(url);
    }


    protected HttpResponse post(String url, String jsonBody) {
        return httpClient.post(url, jsonBody);
    }


    protected HttpResponse put(String url, String jsonBody) {
        return httpClient.put(url, jsonBody);
    }


    protected HttpResponse delete(String url) {
        return httpClient.delete(url);
    }


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

    public void receive(int port) {
        if (httpReceiver == null)
            httpReceiver = new HttpReceiver();

        httpReceiver.start(port, this);
    }

    public HttpResponse sendHttp(int port, String path, HttpMethode method, String body) {
        String url = "http://localhost:" + port + path;

        log("Envoi HTTP " + method + " vers " + url);

        CustomHttpRequest request = CustomHttpRequest.builder()
                .url(url)
                .method(method.name())
                .header("Content-Type", "application/json");

        if (body != null)
            request.body(body);

        return httpClient.execute(request);
    }



    public String getNom() {
        return nom;
    }

    protected ActeurLogger getLogger() {
        return logger;
    }
}
