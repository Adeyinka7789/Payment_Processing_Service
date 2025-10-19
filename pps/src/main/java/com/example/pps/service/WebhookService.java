package com.example.pps.service;

import com.example.pps.dto.FlutterwaveWebhookPayload;
import com.example.pps.dto.PaystackWebhookPayload;
import com.example.pps.entity.Transaction;
import com.example.pps.entity.WebhookEvent;
import com.example.pps.gateway.GatewayProvider;
import com.example.pps.repository.TransactionRepository;
import com.example.pps.repository.WebhookEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {
    private final TransactionRepository transactionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final GatewayProvider paystackGateway;
    private final GatewayProvider flutterwaveGateway;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WebhookService(TransactionRepository transactionRepository,
                          WebhookEventRepository webhookEventRepository,
                          GatewayProvider paystackGateway,
                          GatewayProvider flutterwaveGateway,
                          KafkaTemplate<String, Object> kafkaTemplate) {
        this.transactionRepository = transactionRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.paystackGateway = paystackGateway;
        this.flutterwaveGateway = flutterwaveGateway;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ==========================
    // PAYSTACK WEBHOOK HANDLER
    // ==========================
    @Transactional
    public void processPaystackWebhook(PaystackWebhookPayload payload, String signature) {
        // 1️⃣ Optional: Verify signature
        // paystackGateway.verifyWebhookSignature(payload.toString(), signature);

        // 2️⃣ Find transaction
        Transaction transaction = transactionRepository.findByMerchantRef(payload.getData().getReference())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found for reference: " + payload.getData().getReference()));

        // 3️⃣ Save webhook event — handle duplicates safely
        WebhookEvent event = new WebhookEvent();
        event.setPaymentGateway(WebhookEvent.PaymentGateway.PAYSTACK);
        event.setPayload(payload.toString());
        event.setTransaction(transaction);

        try {
            webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            System.out.println("⚠️ Duplicate Paystack webhook ignored for transaction: " + transaction.getId());
            return;
        }

        // 4️⃣ Update transaction status
        transaction.setStatus(mapPaystackStatus(payload.getData().getStatus()));
        transactionRepository.save(transaction);

        // 5️⃣ Publish to Kafka
        kafkaTemplate.send("merchant-notifications", transaction.getMerchantId().toString(), transaction);
    }

    // ==========================
    // FLUTTERWAVE WEBHOOK HANDLER
    // ==========================
    @Transactional
    public void processFlutterwaveWebhook(FlutterwaveWebhookPayload payload, String signature) {
        // 1️⃣ Optional: Verify signature
        // flutterwaveGateway.verifyWebhookSignature(payload.toString(), signature);

        // 2️⃣ Find transaction
        Transaction transaction = transactionRepository.findByMerchantRef(payload.getTxRef())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found for txRef: " + payload.getTxRef()));

        // 3️⃣ Save webhook event — handle duplicates safely
        WebhookEvent event = new WebhookEvent();
        event.setPaymentGateway(WebhookEvent.PaymentGateway.FLUTTERWAVE);
        event.setPayload(payload.toString());
        event.setTransaction(transaction);

        try {
            webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            System.out.println("⚠️ Duplicate Flutterwave webhook ignored for transaction: " + transaction.getId());
            return;
        }

        // 4️⃣ Update transaction status
        transaction.setStatus(mapFlutterwaveStatus(payload.getStatus()));
        transactionRepository.save(transaction);

        // 5️⃣ Publish to Kafka
        kafkaTemplate.send("merchant-notifications", transaction.getMerchantId().toString(), transaction);
    }

    // ==========================
    // STATUS MAPPERS
    // ==========================
    private Transaction.Status mapPaystackStatus(String status) {
        return switch (status.toLowerCase()) {
            case "success" -> Transaction.Status.COMPLETED;
            case "failed" -> Transaction.Status.FAILED;
            default -> Transaction.Status.PENDING;
        };
    }

    private Transaction.Status mapFlutterwaveStatus(String status) {
        return switch (status.toLowerCase()) {
            case "successful" -> Transaction.Status.COMPLETED;
            case "failed" -> Transaction.Status.FAILED;
            default -> Transaction.Status.PENDING;
        };
    }
}