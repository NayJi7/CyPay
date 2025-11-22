package com.cypay.framework.acteur;

import java.util.HashMap;
import java.util.Map;

public class CustomHttpRequest {
    private String url;
    private String method;
    private String body;
    private Map<String, String> headers;

    public CustomHttpRequest() {
        this.headers = new HashMap<>();
    }

    public static CustomHttpRequest builder() {
        return new CustomHttpRequest();
    }

    public CustomHttpRequest url(String url) {
        this.url = url;
        return this;
    }

    public CustomHttpRequest method(String method) {
        this.method = method == null ? "GET" : method.toUpperCase();
        return this;
    }

    public CustomHttpRequest body(String body) {
        this.body = body;
        return this;
    }

    public CustomHttpRequest header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public CustomHttpRequest headers(Map<String, String> headers) {
        if (headers != null) this.headers.putAll(headers);
        return this;
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public String getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
}
