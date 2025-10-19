package com.example.pps.config;

import com.example.pps.security.ApiKeyAuthFilter;
import com.example.pps.security.RateLimitFilter;
import com.example.pps.security.WebhookSignatureFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Value("${allowed.origins:http://localhost:3000}")
    private String[] allowedOrigins;

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CSRF: Disabled for stateless API (but ensure proper authentication)
                .csrf(csrf -> csrf.disable())

                // CORS: Use custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Session: Stateless for API
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Security Headers
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        // Prevent MIME sniffing
                        .contentTypeOptions(Customizer.withDefaults())
                        // XSS Protection (deprecated but functional)
                        .xssProtection(Customizer.withDefaults())
                        // HSTS: Force HTTPS (enable in production)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // Content Security Policy
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                )

                // Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Health check / actuator endpoints (adjust as needed)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Webhook endpoints: Public but validated by signature filter
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/**").permitAll()

                        // Protected endpoints: Require API key authentication
                        .requestMatchers("/api/v1/transactions/**").authenticated()
                        .requestMatchers("/api/v1/merchants/**").authenticated()

                        // Deny everything else
                        .anyRequest().denyAll()
                )

                // Filter Chain Order (CRITICAL for security):
                // 1. Rate Limiting (before expensive operations)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // 2. Webhook Signature Validation (for webhook endpoints only)
                .addFilterBefore(
                        new WebhookSignatureFilter(paystackSecretKey),
                        UsernamePasswordAuthenticationFilter.class
                )

                // 3. API Key Authentication (for protected endpoints)
                .addFilterBefore(
                        new ApiKeyAuthFilter("x-api-key"),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS Configuration: Restrict origins in production
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins (use environment-specific values)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "x-api-key",
                "Idempotency-Key"
        ));

        // Expose headers (if client needs to read them)
        configuration.setExposedHeaders(List.of("X-Total-Count"));

        // Don't allow credentials with wildcard origins
        configuration.setAllowCredentials(false);

        // Cache preflight for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(redisUrl);
    }
}