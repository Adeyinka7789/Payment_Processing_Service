package com.example.pps.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"transaction_id", "paymentGateway"})
        })
public class WebhookEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT") // Set columnDefinition for potentially large payloads
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGateway paymentGateway;

    @Column(nullable = false)
    private Instant receivedAt;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    public enum PaymentGateway {
        PAYSTACK, FLUTTERWAVE
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public PaymentGateway getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(PaymentGateway paymentGateway) { this.paymentGateway = paymentGateway; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    /**
     * Set the receivedAt field before persisting.
     * Note: createdAt and updatedAt are handled by BaseEntity.
     */
    @PrePersist
    protected void onPrePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}