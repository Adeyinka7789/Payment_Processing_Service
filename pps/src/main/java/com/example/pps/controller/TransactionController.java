package com.example.pps.controller;

import com.example.pps.dto.InitiatePaymentRequest;
import com.example.pps.dto.InitiatePaymentResponse;
import com.example.pps.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        logger.debug("Received payment initiation request: {}", request);
        logger.debug("Idempotency-Key: {}", idempotencyKey);

        InitiatePaymentResponse response =
                transactionService.initiatePayment(request, idempotencyKey, request.getMerchantApiKey());

        logger.debug("Payment initiation response: {}", response);
        return ResponseEntity.ok(response);
    }
}