package com.example.pps.gateway;

import com.example.pps.dto.InitiatePaymentRequest;
import com.example.pps.dto.InitiatePaymentResponse;
import com.example.pps.entity.Transaction;

public interface GatewayProvider {
    InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Transaction transaction);
    void verifyWebhookSignature(String payload, String signature);  // For security
    // Add more methods as needed (e.g., verifyTransaction)
}