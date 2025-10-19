package com.example.pps.service;

import com.example.pps.context.RequestContext;
import com.example.pps.dto.InitiatePaymentRequest;
import com.example.pps.dto.InitiatePaymentResponse;
import com.example.pps.entity.Merchant;
import com.example.pps.entity.Transaction;
import com.example.pps.exception.InvalidMerchantKeyException;
import com.example.pps.gateway.GatewayFactory;
import com.example.pps.repository.MerchantRepository;
import com.example.pps.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final GatewayFactory gatewayFactory;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              MerchantRepository merchantRepository,
                              GatewayFactory gatewayFactory,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
        this.gatewayFactory = gatewayFactory;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request,
                                                   String idempotencyKey,
                                                   String merchantApiKey) {

        String correlationId = RequestContext.getCorrelationId();

        log.info("[correlationId={}] Initiating payment | MerchantRef={} | IdempotencyKey={} | Amount={} {} | Gateway={}",
                correlationId, request.getMerchantRef(), idempotencyKey, request.getAmount(), request.getCurrency(), request.getPaymentGateway());

        // ✅ Authenticate Merchant
        Merchant merchant = merchantRepository.findByApiKey(merchantApiKey)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] Invalid merchant API key: {}", correlationId, merchantApiKey);
                    return new InvalidMerchantKeyException("Invalid Merchant API key.");
                });

        log.debug("[correlationId={}] Merchant authenticated | MerchantID={} | Name={}",
                correlationId, merchant.getId(), merchant.getName());

        // ✅ Check for existing (idempotent) transaction
        Optional<Transaction> existingOpt = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();

            if (existing.getStatus() == Transaction.Status.PENDING ||
                    existing.getStatus() == Transaction.Status.COMPLETED) {

                log.info("[correlationId={}] Duplicate request (idempotent hit) | TransactionID={} | Status={}",
                        correlationId, existing.getId(), existing.getStatus());

                String authUrl = extractAuthorizationUrl(existing);
                return new InitiatePaymentResponse(
                        existing.getId(),
                        existing.getStatus().name(),
                        authUrl
                );
            }
        }

        // ✅ Create new Transaction
        Transaction transaction = new Transaction();
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setMerchantRef(request.getMerchantRef());
        transaction.setAmount(BigDecimal.valueOf(request.getAmount()));
        transaction.setCurrency(parseEnumSafe(Transaction.Currency.class, request.getCurrency(), Transaction.Currency.NGN));
        transaction.setCustomerEmail(request.getCustomerEmail());
        transaction.setPaymentMethod(parseEnumSafe(Transaction.PaymentMethod.class, request.getPaymentMethod(), Transaction.PaymentMethod.CARD));
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setMerchant(merchant);
        transaction.setMerchantId(merchant.getId());

        // ✅ Select payment gateway (default to PAYSTACK)
        Transaction.PaymentGateway gateway = parseEnumSafe(Transaction.PaymentGateway.class,
                request.getPaymentGateway(), Transaction.PaymentGateway.PAYSTACK);
        transaction.setPaymentGateway(gateway);

        log.info("[correlationId={}] Selected payment gateway={} | MerchantRef={}", correlationId, gateway, request.getMerchantRef());

        // ✅ Send request to gateway
        var gatewayProvider = gatewayFactory.getGateway(gateway);

        log.debug("[correlationId={}] Sending request to gateway={} | Amount={} | Customer={}",
                correlationId, gateway, request.getAmount(), request.getCustomerEmail());

        var gatewayResponse = gatewayProvider.initiatePayment(request, transaction);

        // ✅ Update transaction with gateway response
        transaction.setPgTransactionRef(String.valueOf(gatewayResponse.getTransactionId()));
        transaction.setMetadata(String.format("{\"authorizationUrl\": \"%s\"}", gatewayResponse.getAuthorizationUrl()));

        transactionRepository.save(transaction);

        log.info("[correlationId={}] Payment initiated | TxnID={} | PGRef={} | Gateway={} | Status={}",
                correlationId, transaction.getId(), transaction.getPgTransactionRef(), gateway, transaction.getStatus());

        return gatewayResponse;
    }

    // 🔹 Extract authorization URL safely
    private String extractAuthorizationUrl(Transaction transaction) {
        try {
            JsonNode node = objectMapper.readTree(transaction.getMetadata());
            return node.path("authorizationUrl").asText(null);
        } catch (Exception e) {
            log.warn("Failed to parse metadata for TxnID={} | Error={}", transaction.getId(), e.getMessage());
            return null;
        }
    }

    // 🔹 Safe Enum parsing with fallback
    private <E extends Enum<E>> E parseEnumSafe(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value '{}' for {}. Using default: {}", value, enumClass.getSimpleName(), defaultValue);
            return defaultValue;
        }
    }
}