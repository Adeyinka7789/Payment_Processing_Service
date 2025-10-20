package com.example.pps.gateway;

import com.example.pps.dto.InitiatePaymentRequest;
import com.example.pps.dto.InitiatePaymentResponse;
import com.example.pps.entity.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class PaystackGateway implements GatewayProvider {
    private final String secretKey;
    private final RestTemplate restTemplate;

    public PaystackGateway(@Value("${paystack.secret-key:mock-paystack-key}") String secretKey, RestTemplate restTemplate) {
        this.secretKey = secretKey;
        this.restTemplate = restTemplate;
    }

    @Override
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, Transaction transaction) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", transaction.getAmount().multiply(BigDecimal.valueOf(100)).longValue()); // Convert to kobo
        body.put("currency", transaction.getCurrency().toString());
        body.put("email", transaction.getCustomerEmail());
        body.put("reference", transaction.getMerchantRef());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://api.paystack.co/transaction/initialize",
                entity,
                Map.class
        );

        if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
            throw new RuntimeException("Failed to initialize payment with Paystack: " +
                    (response != null ? response.get("message") : "No response"));
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            throw new RuntimeException("Paystack response missing 'data' field");
        }

        InitiatePaymentResponse result = new InitiatePaymentResponse();
        Object idObj = data.get("id");
        result.setTransactionId(idObj != null ?
                (idObj.toString().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") ?
                        java.util.UUID.fromString(idObj.toString()) : java.util.UUID.randomUUID()) :
                java.util.UUID.randomUUID()); // Fallback to random UUID if null or invalid UUID format
        result.setStatus("PENDING");
        result.setAuthorizationUrl((String) data.getOrDefault("authorization_url", "https://api.paystack.co/transaction/initialize" + java.util.UUID.randomUUID()));
        result.setAmount(transaction.getAmount());
        return result;
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