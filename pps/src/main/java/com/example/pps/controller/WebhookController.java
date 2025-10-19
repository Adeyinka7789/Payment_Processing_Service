package com.example.pps.controller;

import com.example.pps.dto.FlutterwaveWebhookPayload;
import com.example.pps.dto.PaystackWebhookPayload;
import com.example.pps.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestBody PaystackWebhookPayload payload,
            @RequestHeader("x-paystack-signature") String signature) {
        webhookService.processPaystackWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/flutterwave")
    public ResponseEntity<Void> handleFlutterwaveWebhook(
            @RequestBody FlutterwaveWebhookPayload payload,
            @RequestHeader("verif-hash") String signature) {
        webhookService.processFlutterwaveWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}