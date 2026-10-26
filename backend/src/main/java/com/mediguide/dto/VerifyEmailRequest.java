package com.mediguide.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for email verification containing the secure verification token.
 */
public class VerifyEmailRequest {

    @NotBlank(message = "Verification token is required")
    private String token;

    public VerifyEmailRequest() {
    }

    public VerifyEmailRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
