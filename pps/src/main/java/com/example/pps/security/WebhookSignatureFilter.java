package com.example.pps.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates webhook signatures from payment gateways (e.g., Paystack)
 * to prevent webhook spoofing and replay attacks.
 */
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureFilter.class);
    private static final String PAYSTACK_SIGNATURE_HEADER = "x-paystack-signature";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private final String secretKey;

    public WebhookSignatureFilter(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Only validate webhook endpoints
        if (!request.getRequestURI().startsWith("/api/v1/webhooks/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Wrap request to allow multiple reads of the body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // Get signature from header
        String receivedSignature = wrappedRequest.getHeader(PAYSTACK_SIGNATURE_HEADER);

        if (receivedSignature == null || receivedSignature.isBlank()) {
            logger.warn("Webhook signature missing from request: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "Missing webhook signature");
            return;
        }

        // Read request body
        byte[] requestBody = wrappedRequest.getContentAsByteArray();
        if (requestBody.length == 0) {
            // Trigger body read
            wrappedRequest.getReader();
            requestBody = wrappedRequest.getContentAsByteArray();
        }

        // Compute expected signature
        String computedSignature = computeHmacSha512(requestBody, secretKey);

        // Constant-time comparison to prevent timing attacks
        if (!constantTimeEquals(receivedSignature, computedSignature)) {
            logger.warn("Invalid webhook signature. Expected: {}, Received: {}",
                    computedSignature, receivedSignature);
            sendUnauthorizedResponse(response, "Invalid webhook signature");
            return;
        }

        // Signature valid, proceed
        logger.debug("Webhook signature validated successfully");
        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * Computes HMAC-SHA512 signature for webhook validation
     */
    private String computeHmacSha512(byte[] data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA512
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data);
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error computing HMAC signature", e);
            throw new RuntimeException("Failed to compute webhook signature", e);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("""
            {
              "error": "Unauthorized",
              "message": "%s"
            }
        """, message));
    }
}