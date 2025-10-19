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
        // Optional signature verification
        // paystackGateway.verifyWebhookSignature(payload.toString(), signature);

        // 1️⃣ Find transaction
        Transaction transaction = transactionRepository.findByMerchantRef(payload.getData().getReference())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found for reference: " + payload.getData().getReference()));

        // 2️⃣ Check if we’ve already processed this webhook
        if (webhookEventRepository.existsByTransactionAndPaymentGateway(transaction, WebhookEvent.PaymentGateway.PAYSTACK)) {
            System.out.println("⚠️ Duplicate Paystack webhook ignored for transaction: " + transaction.getId());
            return;
        }

        // 3️⃣ Update transaction status FIRST
        transaction.setStatus(mapPaystackStatus(payload.getData().getStatus()));
        transactionRepository.saveAndFlush(transaction); // flush ensures the change is visible to the next save

        // 4️⃣ Then persist webhook event
        WebhookEvent event = new WebhookEvent();
        event.setPaymentGateway(WebhookEvent.PaymentGateway.PAYSTACK);
        event.setPayload(payload.toString());
        event.setTransaction(transaction);
        webhookEventRepository.saveAndFlush(event);

        // 5️⃣ Publish to Kafka (outside DB transaction, optional)
        kafkaTemplate.send("merchant-notifications", transaction.getMerchantId().toString(), transaction);
    }

    // ==========================
    // FLUTTERWAVE WEBHOOK HANDLER
    // ==========================
    @Transactional
    public void processFlutterwaveWebhook(FlutterwaveWebhookPayload payload, String signature) {
        // flutterwaveGateway.verifyWebhookSignature(payload.toString(), signature);

        Transaction transaction = transactionRepository.findByMerchantRef(payload.getTxRef())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found for txRef: " + payload.getTxRef()));

        if (webhookEventRepository.existsByTransactionAndPaymentGateway(transaction, WebhookEvent.PaymentGateway.FLUTTERWAVE)) {
            System.out.println("⚠️ Duplicate Flutterwave webhook ignored for transaction: " + transaction.getId());
            return;
        }

        transaction.setStatus(mapFlutterwaveStatus(payload.getStatus()));
        transactionRepository.saveAndFlush(transaction);

        WebhookEvent event = new WebhookEvent();
        event.setPaymentGateway(WebhookEvent.PaymentGateway.FLUTTERWAVE);
        event.setPayload(payload.toString());
        event.setTransaction(transaction);
        webhookEventRepository.saveAndFlush(event);

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