package com.example.pps.context;

import java.util.UUID;

public class RequestContext {
    private static final ThreadLocal<String> correlationId = new ThreadLocal<>();

    public static void setCorrelationId(String id) {
        correlationId.set(id);
    }

    public static String getCorrelationId() {
        return correlationId.get();
    }

    public static void clear() {
        correlationId.remove();
    }

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}