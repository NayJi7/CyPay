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

    // ✅ AJOUT : Référence au superviseur
    protected Acteur<?> supervisor;

    public Acteur(String nom) {
        this(nom, false, null, null, null);
    }

    public Acteur(String nom, boolean logToDb, String jdbcUrl, String dbUser, String dbPassword) {
        this.nom = nom;
        this.mailbox = new LinkedBlockingQueue<>();
        this.running = true;
        this.logger = new ActeurLogger(nom, logToDb, jdbcUrl, dbUser, dbPassword);
        this.httpClient = new ActeurHttpClient(logger);
    }

    // ✅ AJOUT : Configurer le superviseur
    public void setSupervisor(Acteur<?> supervisor) {
        this.supervisor = supervisor;
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
     * ✅ MODIFIÉ : Gestion des erreurs avec notification au superviseur
     */
    @Override
    public void run() {
        while (running) {
            try {
                Message<T> message = mailbox.take();
                traiterMessage(message.getContenu());
            } catch (InterruptedException e) {
                logger.erreur("[ERROR] Thread interrompu", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.erreur("[ERROR] Erreur lors du traitement du message", e);
                if (supervisor != null) {
                    try {
                        notifierSuperviseur(e);
                        logger.info("[NOTIFY] Superviseur notifié de l'erreur");
                    } catch (Exception notifyError) {
                        logger.erreur("[ERROR] Impossible de notifier le superviseur", notifyError);
                    }
                }
            }
        }
        logger.info("[STOP] Acteur arrêté");
    }

    /**
     * ✅ AJOUT : Notifie le superviseur d'une défaillance
     */
    private void notifierSuperviseur(Exception e) {
        try {
            // Créer dynamiquement un message ActorFailed via réflexion
            Class<?> messagesClass = Class.forName("com.cypay.logs.acteur.Messages");
            Class<?>[] innerClasses = messagesClass.getDeclaredClasses();
            for (Class<?> innerClass : innerClasses) {
                if (innerClass.getSimpleName().equals("ActorFailed")) {
                    Object failureMessage = innerClass
                            .getDeclaredConstructors()[0]
                            .newInstance(this.nom, e, System.currentTimeMillis());
                    Message<?> message = new Message<>(this.nom, failureMessage);
                    supervisor.envoyer((Message) message);
                    return;
                }
            }
        } catch (Exception reflectionError) {
            logger.erreur("[ERROR] Erreur lors de la notification du superviseur", reflectionError);
        }
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

    // ✅ AJOUT : Vérifie si l'acteur est actif
    public boolean estActif() {
        return running && thread != null && thread.isAlive();
    }

    public void receive(int port) {
        if (httpReceiver == null)
            httpReceiver = new HttpReceiver();

        httpReceiver.start(port, this);
    }

    public HttpResponse sendHttp(int port, String path, HttpMethode method, String body) {
        String url = "http://localhost:" + port + path;

        log("[HTTP-OUT] " + method + " -> " + url);

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
/*
    * Envoie une requête HTTP vers un autre microservice
     *
             * @param host Hostname ou IP (ex: "payment-service" ou "localhost")
     * @param port Port du microservice (ex: 8082)
     * @param path Chemin de l'endpoint (ex: "/api/payment")
            * @param method Méthode HTTP (GET, POST, PUT, DELETE)
     * @param jsonBody Corps JSON (peut être null pour GET/DELETE)
     * @return HttpResponse avec status, body, headers
     *
             * @example
     * HttpResponse response = sendToService(
            *     "payment-service", 8082,
            *     "/api/process",
            *     HttpMethode.POST,
            *     "{\"amount\": 1000}"
            * );
     */
    protected HttpResponse sendToService(String host, int port, String path,
                                         HttpMethode method, String jsonBody) {

        String url = buildUrl(host, port, path);

        log("[OUT] -> " + host + ":" + port + " " + method + " " + path);

        HttpResponse response;

        switch (method) {
            case GET -> response = get(url);
            case POST -> response = post(url, jsonBody);
            case PUT -> response = put(url, jsonBody);
            case DELETE -> response = delete(url);
            case PATCH -> response = patch(url, jsonBody);
            default -> throw new IllegalArgumentException("Method not supported: " + method);
        }

        if (response.isSuccess()) {
            log("[RESP] [SUCCESS] " + response.getStatusCode());
        } else {
            log("[RESP] [ERROR] " + response.getStatusCode());
        }

        return response;
    }

    /**
     * Envoie une requête HTTP vers un autre microservice (version simplifiée avec localhost)
     *
     * @param port Port du microservice local (ex: 8082)
     * @param path Chemin de l'endpoint (ex: "/api/payment")
     * @param method Méthode HTTP
     * @param jsonBody Corps JSON
     * @return HttpResponse
     *
     * @example
     * HttpResponse response = sendToService(8082, "/api/payment", HttpMethode.POST, json);
     */
    protected HttpResponse sendToService(int port, String path,
                                         HttpMethode method, String jsonBody) {
        return sendToService("localhost", port, path, method, jsonBody);
    }

    /**
     * Envoie un message JSON vers un autre acteur distant (méthode POST)
     *
     * @param host Hostname du microservice distant
     * @param port Port du microservice
     * @param acteurName Nom de l'acteur distant
     * @param messageJson Message sérialisé en JSON
     * @return HttpResponse
     *
     * @example
     * String json = gson.toJson(new PaymentRequest("user123", 1000));
     * HttpResponse response = sendToActeur("payment-service", 8082, "PaymentActeur", json);
     */
    protected HttpResponse sendToActeur(String host, int port,
                                        String acteurName, String messageJson) {

        String path = "/acteur/" + acteurName + "/message";

        log("[MSG-OUT] -> Remote Actor: " + acteurName);

        return sendToService(host, port, path, HttpMethode.POST, messageJson);
    }

    /**
     * Envoie un message JSON vers un acteur distant local (localhost)
     *
     * @param port Port du microservice
     * @param acteurName Nom de l'acteur distant
     * @param messageJson Message JSON
     * @return HttpResponse
     */
    protected HttpResponse sendToActeur(int port, String acteurName, String messageJson) {
        return sendToActeur("localhost", port, acteurName, messageJson);
    }

    /**
     * Envoie un objet vers un acteur distant (sérialisation automatique en JSON)
     *
     * @param host Hostname
     * @param port Port
     * @param acteurName Nom de l'acteur distant
     * @param message Objet à sérialiser
     * @return HttpResponse
     *
     * @example
     * PaymentRequest request = new PaymentRequest("user123", 1000);
     * HttpResponse response = sendToActeur("payment-service", 8082, "PaymentActeur", request);
     */
    protected HttpResponse sendToActeur(String host, int port,
                                        String acteurName, Object message) {

        // Sérialiser l'objet en JSON
        String json = serializeToJson(message);

        return sendToActeur(host, port, acteurName, json);
    }

    /**
     * Envoie un objet vers un acteur distant local
     */
    protected HttpResponse sendToActeur(int port, String acteurName, Object message) {
        return sendToActeur("localhost", port, acteurName, message);
    }

    /**
     * GET vers un service distant
     */
    protected HttpResponse getFromService(String host, int port, String path) {
        return sendToService(host, port, path, HttpMethode.GET, null);
    }

    /**
     * GET vers un service local
     */
    protected HttpResponse getFromService(int port, String path) {
        return getFromService("localhost", port, path);
    }

    /**
     * POST vers un service distant
     */
    protected HttpResponse postToService(String host, int port, String path, String json) {
        return sendToService(host, port, path, HttpMethode.POST, json);
    }

    /**
     * POST vers un service local
     */
    protected HttpResponse postToService(int port, String path, String json) {
        return postToService("localhost", port, path, json);
    }

    /**
     * PUT vers un service distant
     */
    protected HttpResponse putToService(String host, int port, String path, String json) {
        return sendToService(host, port, path, HttpMethode.PUT, json);
    }

    /**
     * DELETE vers un service distant
     */
    protected HttpResponse deleteFromService(String host, int port, String path) {
        return sendToService(host, port, path, HttpMethode.DELETE, null);
    }

    /**
     * PATCH vers un service distant (à ajouter aussi dans ActeurHttpClient)
     */
    protected HttpResponse patch(String url, String jsonBody) {
        return httpClient.patch(url, jsonBody);
    }

    /**
     * Construit l'URL complète
     */
    private String buildUrl(String host, int port, String path) {
        // Ajouter / au début du path si absent
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return String.format("http://%s:%d%s", host, port, path);
    }

    /**
     * Sérialise un objet en JSON
     * Utilise Gson si disponible, sinon toString()
     */
    private String serializeToJson(Object obj) {
        try {
            // Essayer avec Gson
            Class<?> gsonClass = Class.forName("com.google.gson.Gson");
            Object gson = gsonClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method toJson = gsonClass.getMethod("toJson", Object.class);
            return (String) toJson.invoke(gson, obj);

        } catch (Exception e) {
            // Fallback : toString() ou format simple
            log("[WARN] Gson not available, using toString()");
            return String.format("{\"data\": \"%s\"}", obj.toString());
        }
    }

    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    /**
     * Vérifie si la réponse est un succès et log le résultat
     */
    protected boolean isResponseSuccess(HttpResponse response, String operationName) {
        if (response.isSuccess()) {
            log("[SUCCESS] " + operationName + " : (" + response.getStatusCode() + ")");
            return true;
        } else {
            log("[FAILURE] " + operationName + " : (" + response.getStatusCode() + ")");
            log("   Body: " + response.getBody());
            return false;
        }
    }

    /**
     * Extrait un champ JSON simple de la réponse
     * (implémentation basique, pour production utilisez Jackson/Gson)
     */
    protected String extractJsonField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);

            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log("[WARN] JSON extraction error : " + fieldName);
        }
        return null;
    }

    /**
     * Ping un service pour vérifier s'il est disponible
     */
    protected boolean pingService(String host, int port) {
        return pingService(host, port, "/health");
    }

    /**
     * Ping un service sur un endpoint spécifique
     */
    protected boolean pingService(String host, int port, String healthPath) {
        try {
            HttpResponse response = getFromService(host, port, healthPath);
            return response.isSuccess();
        } catch (Exception e) {
            log("[WARN] Service " + host + ":" + port + " unavailable");
            return false;
        }
    }

    /**
     * Retry automatique avec backoff exponentiel
     */
    protected HttpResponse sendToServiceWithRetry(String host, int port, String path,
                                                  HttpMethode method, String jsonBody,
                                                  int maxRetries) {

        HttpResponse response = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                response = sendToService(host, port, path, method, jsonBody);

                if (response.isSuccess()) {
                    return response;
                }

                log("[WARN] Attempt " + attempt + "/" + maxRetries + " failed");

                if (attempt < maxRetries) {
                    long delay = (long) Math.pow(2, attempt - 1) * 1000; // Backoff exponentiel
                    Thread.sleep(delay);
                }

            } catch (Exception e) {
                logErreur("Erreur tentative " + attempt, e);

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logErreur("[FAILURE] Max retries exceeded (" + maxRetries + ")",
                new Exception("Max retries exceeded"));

        return response;
    }

    /**
     * Circuit breaker simple
     */
    private static class CircuitBreaker {
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private static final int FAILURE_THRESHOLD = 5;
        private static final long TIMEOUT_MS = 60000;

        public boolean isOpen() {
            if (failureCount >= FAILURE_THRESHOLD) {
                long elapsed = System.currentTimeMillis() - lastFailureTime;
                if (elapsed < TIMEOUT_MS) {
                    return true;
                } else {
                    reset();
                }
            }
            return false;
        }

        public void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
        }

        public void reset() {
            failureCount = 0;
        }
    }

    private final java.util.Map<String, CircuitBreaker> circuitBreakers =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Envoie avec circuit breaker automatique
     */
    protected HttpResponse sendToServiceWithCircuitBreaker(String host, int port,
                                                           String path, HttpMethode method,
                                                           String jsonBody) {

        String serviceKey = host + ":" + port;
        CircuitBreaker breaker = circuitBreakers.computeIfAbsent(serviceKey,
                k -> new CircuitBreaker());

        if (breaker.isOpen()) {
            log("[WARN] Circuit breaker OPEN for " + serviceKey);
            // Retourner une erreur 503
            return new HttpResponse(503, "{\"error\": \"Service unavailable (circuit open)\"}",
                    java.util.Map.of());
        }

        try {
            HttpResponse response = sendToService(host, port, path, method, jsonBody);

            if (response.isSuccess()) {
                breaker.reset();
            } else {
                breaker.recordFailure();
            }

            return response;

        } catch (Exception e) {
            breaker.recordFailure();
            throw e;
        }
    }
}