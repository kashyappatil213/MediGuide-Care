package com.mediguide.dto;

import com.mediguide.model.User;

/**
 * Response DTO returned to an authenticated patient viewing or updating their personal contact details.
 * Excludes sensitive credentials and internal database primary keys.
 */
public class PatientProfileResponse {

    private String publicId;
    private String fullName;
    private String email;
    private String phoneNumber;

    public PatientProfileResponse() {
    }

    public PatientProfileResponse(String publicId, String fullName, String email, String phoneNumber) {
        this.publicId = publicId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public static PatientProfileResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new PatientProfileResponse(
                user.getPublicId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
