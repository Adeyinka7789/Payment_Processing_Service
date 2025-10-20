package com.example.pps.service;

import com.example.pps.dto.AuthResponse;
import com.example.pps.dto.LoginRequest;
import com.example.pps.dto.RegisterRequest;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of AuthService to satisfy the API contract
 * expected by the frontend. In a real application, this would interact
 * with a database for user validation and generate a real JWT.
 *
 * NOTE: Credentials updated to match the test data (ppsuser/ppspass)
 * seen in the application logs to ensure successful mock authentication.
 */
@Service
public class MockAuthService implements AuthService {

    // Updated token and ID for a slightly more unique mock response
    private static final String MOCK_TOKEN = "mock-jwt-token-5833f50f-c2f1-4e75-befd-33d31fdfc751";
    private static final String MOCK_MERCHANT_ID = "merch_5833f50f";

    // *** FIXED: Updated to match client's test credentials from logs (ppsuser/ppspass) ***
    private static final String MOCK_VALID_EMAIL = "ppsuser";
    private static final String MOCK_VALID_PASSWORD = "ppspass";

    @Override
    public String register(RegisterRequest request) {
        // Mock registration success.
        // In a real app, this would hash the password and save the user.
        return request.name();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Mock validation against hardcoded credentials.
        if (MOCK_VALID_EMAIL.equals(request.email()) && MOCK_VALID_PASSWORD.equals(request.password())) {
            // Mock successful login response, matching the frontend expectation.
            return new AuthResponse(MOCK_TOKEN, MOCK_MERCHANT_ID);
        }
        // Throws exception if credentials don't match
        throw new IllegalArgumentException("Invalid email or password.");
    }
}