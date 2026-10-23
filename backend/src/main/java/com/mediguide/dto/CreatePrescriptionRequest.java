package com.mediguide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a prescription item.
 */
public class CreatePrescriptionRequest {

    @NotBlank(message = "Medication name is required")
    @Size(max = 150, message = "Medication name must not exceed 150 characters")
    private String medicationName;

    @NotBlank(message = "Dosage is required")
    @Size(max = 100, message = "Dosage must not exceed 100 characters")
    private String dosage;

    @NotBlank(message = "Instructions are required")
    @Size(max = 255, message = "Instructions must not exceed 255 characters")
    private String instructions;

    @Size(max = 100, message = "Duration must not exceed 100 characters")
    private String duration;

    public CreatePrescriptionRequest() {
    }

    public CreatePrescriptionRequest(String medicationName, String dosage, String instructions, String duration) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.instructions = instructions;
        this.duration = duration;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
