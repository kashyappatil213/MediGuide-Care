package com.mediguide.dto;

/**
 * Response payload returned upon successful email verification.
 */
public class VerifyEmailResponse {

    private String message;
    private boolean verified;
    private String email;

    public VerifyEmailResponse() {
    }

    public VerifyEmailResponse(String message, boolean verified, String email) {
        this.message = message;
        this.verified = verified;
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
