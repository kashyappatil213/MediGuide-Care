package com.mediguide.controller;

import com.mediguide.dto.LoginRequest;
import com.mediguide.dto.LoginResponse;
import com.mediguide.dto.RegisterRequest;
import com.mediguide.dto.ResendVerificationRequest;
import com.mediguide.dto.ResendVerificationResponse;
import com.mediguide.dto.UserResponse;
import com.mediguide.dto.VerifyEmailRequest;
import com.mediguide.dto.VerifyEmailResponse;
import com.mediguide.service.AuthService;
import com.mediguide.service.EmailVerificationService;
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
 * REST controller handling user registration, login authentication, and email verification.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Registration", description = "Endpoints for user registration, login, and email verification")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
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

    /**
     * Verifies a user's email address using a single-use verification token.
     *
     * @param request token payload
     * @return 200 OK with confirmation details
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify Email Address", description = "Validates single-use token and marks account as email verified")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        VerifyEmailResponse response = emailVerificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Resends an email verification link with rate-limiting and anti-enumeration protection.
     *
     * @param request email payload
     * @return 200 OK with neutral confirmation message
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend Verification Email", description = "Sends a new verification token link subject to cooldown limits")
    public ResponseEntity<ResendVerificationResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        ResendVerificationResponse response = emailVerificationService.resendVerification(request.getEmail());
        return ResponseEntity.ok(response);
    }
}
