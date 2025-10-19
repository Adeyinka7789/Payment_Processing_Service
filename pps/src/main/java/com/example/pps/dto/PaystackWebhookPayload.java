package com.example.pps.dto;

import lombok.Data;

@Data
public class PaystackWebhookPayload {
    private String event;
    private Data data;

    @lombok.Data
    public static class Data {
        private String reference;
        private String status;
        private String amount;
        private String currency;
        private String customerEmail;
    }
}