package com.mediguide.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for generating an AI-assisted clinical history summary for a patient.
 */
public class ClinicalHistorySummaryRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    public ClinicalHistorySummaryRequest() {
    }

    public ClinicalHistorySummaryRequest(Long patientId) {
        this.patientId = patientId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
}
