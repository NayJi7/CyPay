package com.cypay.framework.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Message représentant une requête HTTP entrante
 * Utilisé dans le mode simple de HttpReceiver
 */
public class HttpIncomingMessage {

    private final String method;
    private final String path;
    private final String query;
    private final String body;
    private final Map<String, String> headers;

    /**
     * Constructeur simple (rétrocompatible)
     */
    public HttpIncomingMessage(String method, String path, String body) {
        this(method, path, null, body, new HashMap<>());
    }

    /**
     * Constructeur complet
     */
    public HttpIncomingMessage(String method, String path, String query,
                               String body, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.query = query;
        this.body = body;
        this.headers = headers != null ? headers : new HashMap<>();
    }

    // Getters

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public String query() {
        return query;
    }

    public String body() {
        return body;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * Parse les paramètres de requête depuis query string
     */
    public Map<String, String> getQueryParams() {
        Map<String, String> params = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }

        return params;
    }

    /**
     * Vérifie si c'est une méthode spécifique
     */
    public boolean isMethod(String method) {
        return this.method.equalsIgnoreCase(method);
    }

    /**
     * Vérifie si le path correspond
     */
    public boolean isPath(String path) {
        return this.path.equals(path);
    }

    /**
     * Vérifie si le path commence par un préfixe
     */
    public boolean pathStartsWith(String prefix) {
        return this.path.startsWith(prefix);
    }

    @Override
    public String toString() {
        return String.format("HttpIncomingMessage{method='%s', path='%s', query='%s', bodyLength=%d}",
                method, path, query, body != null ? body.length() : 0);
    }
}