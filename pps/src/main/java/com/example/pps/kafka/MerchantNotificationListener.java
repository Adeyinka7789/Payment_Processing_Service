package com.example.pps.kafka;

import com.example.pps.entity.Transaction;
import com.example.pps.repository.MerchantRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class MerchantNotificationListener {
    private final RestTemplate restTemplate;
    private final MerchantRepository merchantRepository;

    public MerchantNotificationListener(RestTemplate restTemplate, MerchantRepository merchantRepository) {
        this.restTemplate = restTemplate;
        this.merchantRepository = merchantRepository;
    }

    @KafkaListener(topics = "merchant-notifications", groupId = "${spring.kafka.consumer.group-id}")
    public void handleNotification(Transaction transaction) {
        merchantRepository.findById(transaction.getMerchantId())
                .ifPresent(merchant -> {
                    String webhookUrl = merchant.getWebhookUrl();
                    try {
                        restTemplate.postForObject(webhookUrl, transaction, String.class);
                    } catch (Exception e) {
                        System.err.println("Failed to send notification to " + webhookUrl + ": " + e.getMessage());
                    }
                });
    }
}