package com.example.pps.repository;

import com.example.pps.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Optional<Transaction> findByMerchantRef(String merchantRef);
    Optional<Transaction> findByMerchantRefAndMerchantId(String merchantRef, UUID merchantId);
    Optional<Transaction> findByPgTransactionRefAndPaymentGateway(String pgTransactionRef, Transaction.PaymentGateway paymentGateway);
}