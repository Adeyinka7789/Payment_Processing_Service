package com.example.pps.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(nullable = false)
    private String name;

    @Column
    private String webhookUrl; // For merchant notifications

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}