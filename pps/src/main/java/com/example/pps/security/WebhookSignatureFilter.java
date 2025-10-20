package com.example.pps.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

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

        // Wrap request to cache body
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        byte[] requestBody = cachedRequest.getCachedBody();

        if (requestBody.length == 0) {
            logger.warn("Empty webhook body received for URI: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "Empty webhook body");
            return;
        }

        logger.debug("Webhook raw body: {}", new String(requestBody, StandardCharsets.UTF_8));

        // Extract signature from header
        String receivedSignature = cachedRequest.getHeader(PAYSTACK_SIGNATURE_HEADER);
        if (receivedSignature == null || receivedSignature.isBlank()) {
            logger.warn("Webhook signature missing from request: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "Missing webhook signature");
            return;
        }

        // Compute HMAC
        String computedSignature = computeHmacSha512(requestBody, secretKey);

        if (!constantTimeEquals(receivedSignature, computedSignature)) {
            logger.warn("Invalid webhook signature. Expected: {}, Received: {}", computedSignature, receivedSignature);
            sendUnauthorizedResponse(response, "Invalid webhook signature");
            return;
        }

        logger.info("✅ Webhook signature validated successfully");
        filterChain.doFilter(cachedRequest, response);
    }

    private String computeHmacSha512(byte[] data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data);
            return HexFormat.of().formatHex(hmacBytes).toLowerCase();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute webhook signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) result |= aBytes[i] ^ bBytes[i];
        return result == 0;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
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