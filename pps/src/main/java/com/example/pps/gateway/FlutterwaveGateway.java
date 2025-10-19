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
public class FlutterwaveGateway implements GatewayProvider {
    private final String secretKey;

    public FlutterwaveGateway(@Value("${flutterwave.secret-key:mock-flutterwave-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Transaction transaction) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setTransactionId(UUID.randomUUID());
        response.setStatus("PENDING");
        response.setAuthorizationUrl("https://mock.flutterwave.com/v3/" + UUID.randomUUID());
        return response;
    }

    @Override
    public void verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            if (!computedSignature.equals(signature)) {
                throw new SecurityException("Invalid Flutterwave webhook signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Signature verification failed", e);
        }
    }
}