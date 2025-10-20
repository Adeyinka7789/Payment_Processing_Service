package com.example.pps.config;

import com.example.pps.security.ApiKeyAuthFilter;
import com.example.pps.security.RateLimitFilter;
import com.example.pps.security.WebhookSignatureFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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

    @Value("${paystack.secret-key:mock-paystack-key}")
    private String paystackSecretKey;

    @Value("${allowed.origins:http://localhost:3000,http://localhost:63342}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter redisRateLimitFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                        .xssProtection(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/api/v1/transactions/**").authenticated()
                        .requestMatchers("/api/v1/merchants/**").authenticated()
                        .anyRequest().denyAll()
                )

                // Webhook signature validation first
                .addFilterBefore(new WebhookSignatureFilter(paystackSecretKey), UsernamePasswordAuthenticationFilter.class)

                // Redis-backed rate limiting second
                .addFilterBefore(redisRateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // API Key authentication after rate limiting
                .addFilterAfter(new ApiKeyAuthFilter("x-api-key"), RateLimitFilter.class);

        return http.build();
    }

    @Bean(name = "redisRateLimitFilter")
    public RateLimitFilter redisRateLimitFilter() {
        return new RateLimitFilter(redisUrl);
    }

    @Bean
    public FilterRegistrationBean<WebhookSignatureFilter> webhookSignatureFilterRegistration() {
        FilterRegistrationBean<WebhookSignatureFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new WebhookSignatureFilter(paystackSecretKey));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.addUrlPatterns("/api/v1/webhooks/*");
        return registrationBean;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "x-api-key", "Idempotency-Key"));
        configuration.setExposedHeaders(List.of("X-Total-Count"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}