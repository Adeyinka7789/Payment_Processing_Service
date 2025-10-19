package com.example.pps.repository;

import com.example.pps.entity.Transaction;
import com.example.pps.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    boolean existsByPayload(String payload);

    boolean existsByTransactionAndPaymentGateway(Transaction transaction, WebhookEvent.PaymentGateway paymentGateway);
}