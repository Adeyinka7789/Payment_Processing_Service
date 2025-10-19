package com.example.pps.dto;

import java.util.UUID;

public class InitiatePaymentResponse {
    private UUID transactionId;
    private String status;
    private String authorizationUrl;

    // No-arg constructor (required for frameworks like Jackson)
    public InitiatePaymentResponse() {
    }

    // Constructor for idempotency response
    public InitiatePaymentResponse(UUID transactionId, String status, String authorizationUrl) {
        this.transactionId = transactionId;
        this.status = status;
        this.authorizationUrl = authorizationUrl;
    }

    // Getters and Setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAuthorizationUrl() { return authorizationUrl; }
    public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }
}