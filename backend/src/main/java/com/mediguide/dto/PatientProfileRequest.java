package com.mediguide.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a patient's personal contact details.
 */
public class PatientProfileRequest {

    @Pattern(
            regexp = "^$|^[+]?[0-9\\s\\-()]{7,20}$",
            message = "Phone number must be a valid contact number containing 7 to 20 digits"
    )
    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    public PatientProfileRequest() {
    }

    public PatientProfileRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
