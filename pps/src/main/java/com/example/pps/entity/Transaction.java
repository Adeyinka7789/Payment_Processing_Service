package com.example.pps.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
public class Transaction extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "merchant_ref", nullable = false)
    private String merchantRef;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "pg_transaction_ref")
    private String pgTransactionRef;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, columnDefinition = "varchar(255)")
    private Currency currency;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, columnDefinition = "varchar(255)")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", nullable = false)
    private PaymentGateway paymentGateway;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false)
    private Merchant merchant;

    public enum PaymentGateway {
        PAYSTACK, FLUTTERWAVE
    }

    public enum Status {
        PENDING, COMPLETED, FAILED
    }

    public enum Currency {
        NGN, USD, EUR
    }

    public enum PaymentMethod {
        CARD, BANK_TRANSFER
    }
}