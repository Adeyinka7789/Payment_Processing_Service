package com.example.pps.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a valid ISO 3-letter code (e.g., NGN)")
    private String currency;

    @NotBlank(message = "Merchant reference is required")
    private String merchantRef;

    @Email(message = "Customer email must be valid")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Payment gateway is required")
    private String paymentGateway;

    @NotBlank(message = "Merchant API key is required")
    private String merchantApiKey;
}