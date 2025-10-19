package com.example.pps.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class DebugConfig {
    private static final Logger log = LoggerFactory.getLogger(DebugConfig.class);

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @PostConstruct
    public void logDbPassword() {
        log.info("Resolved DB_PASSWORD: {}", dbPassword);  // Check log for what Spring is using
    }
}