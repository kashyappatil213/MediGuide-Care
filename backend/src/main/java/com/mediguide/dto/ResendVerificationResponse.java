package com.mediguide.dto;

/**
 * Uniform response payload for resend verification email requests.
 */
public class ResendVerificationResponse {

    private String message;

    public ResendVerificationResponse() {
    }

    public ResendVerificationResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
