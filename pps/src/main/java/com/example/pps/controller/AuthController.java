package com.example.pps.controller;

import com.example.pps.dto.AuthResponse;
import com.example.pps.dto.LoginRequest;
import com.example.pps.dto.RegisterRequest;
import com.example.pps.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling merchant authentication requests (login and registration).
 * This controller's paths are permitted access in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handles merchant registration.
     * Maps to POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterRequest> register(@RequestBody RegisterRequest request) {
        // Calls the mock service to simulate account creation
        authService.register(request);
        // Returns the request object back for simplicity and to match mock flow
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    /**
     * Handles merchant login and authentication.
     * Maps to POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Calls the mock service to validate credentials and get the token/ID
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
