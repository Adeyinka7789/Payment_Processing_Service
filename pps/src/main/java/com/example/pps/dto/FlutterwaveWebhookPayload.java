package com.example.pps.dto;

import lombok.Data;

@Data
public class FlutterwaveWebhookPayload {
    private String status;
    private String txRef;
    private String flwRef;
    private String amount;
    private String currency;
    private String customerEmail;
}