package com.example.pps.service;

import com.example.pps.dto.AuthResponse;
import com.example.pps.dto.LoginRequest;
import com.example.pps.dto.RegisterRequest;

/**
 * Interface for the Authentication Service.
 */
public interface AuthService {
    /**
     * Registers a new merchant.
     * @param request the registration details
     * @return The registered merchant's name (for mock purposes)
     */
    String register(RegisterRequest request);

    /**
     * Authenticates a merchant.
     * @param request the login credentials
     * @return The AuthResponse containing the token and merchant ID
     */
    AuthResponse login(LoginRequest request);
}
