package com.example.pps.consumer;

import com.example.pps.entity.Transaction;
import com.example.pps.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final TransactionRepository transactionRepository;

    public PaymentEventConsumer(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "merchant-notifications", groupId = "pps-group")
    public void handlePaymentEvent(Transaction transaction) {
        logger.info("Processing payment event for TxnID: {}", transaction.getId());
        if (transaction.getStatus() == Transaction.Status.PENDING) {
            transaction.setStatus(Transaction.Status.COMPLETED);
            transactionRepository.save(transaction);
            logger.info("Updated transaction {} status to COMPLETED", transaction.getId());
        } else {
            logger.warn("Transaction {} already processed with status: {}", transaction.getId(), transaction.getStatus());
        }
    }
}