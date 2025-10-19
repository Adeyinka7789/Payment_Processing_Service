package com.example.pps.gateway;

import com.example.pps.dto.InitiatePaymentRequest;
import com.example.pps.dto.InitiatePaymentResponse;
import com.example.pps.entity.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Component
public class PaystackGateway implements GatewayProvider {
    private final String secretKey;

    public PaystackGateway(@Value("${paystack.secret-key:mock-paystack-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Transaction transaction) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setTransactionId(UUID.randomUUID());
        response.setStatus("PENDING");
        response.setAuthorizationUrl("https://mock.paystack.co/pay/" + UUID.randomUUID());
        return response;
    }

    @Override
    public void verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            if (!computedSignature.equals(signature)) {
                throw new SecurityException("Invalid Paystack webhook signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Signature verification failed", e);
        }
    }
}