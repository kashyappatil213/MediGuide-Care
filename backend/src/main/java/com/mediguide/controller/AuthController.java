package com.mediguide.controller;

import com.mediguide.dto.LoginRequest;
import com.mediguide.dto.LoginResponse;
import com.mediguide.dto.RegisterRequest;
import com.mediguide.dto.UserResponse;
import com.mediguide.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling user registration and login authentication.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Registration", description = "Endpoints for new user registration and login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new patient or doctor account in MediGuide Care.
     *
     * @param request validated registration details
     * @return 201 Created with safe public user details
     */
    @PostMapping("/register")
    @Operation(summary = "Register User", description = "Public self-service registration for PATIENT and DOCTOR accounts")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates user credentials and issues a signed JWT access token.
     *
     * @param request validated login credentials
     * @return 200 OK with LoginResponse containing access token and user info
     */
    @PostMapping("/login")
    @Operation(summary = "Login User", description = "Verifies credentials and returns a signed JWT access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
