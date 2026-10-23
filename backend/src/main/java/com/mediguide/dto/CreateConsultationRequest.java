package com.mediguide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating or updating a consultation record,
 * including optional recommended follow-up date and notes.
 */
public class CreateConsultationRequest {

    @NotBlank(message = "Consultation notes or diagnosis are required")
    @Size(max = 2000, message = "Consultation notes must not exceed 2000 characters")
    private String notes;

    @Size(max = 2000, message = "Treatment advice must not exceed 2000 characters")
    private String treatment;

    private LocalDate followUpDate;

    @Size(max = 1000, message = "Follow-up notes must not exceed 1000 characters")
    private String followUpNotes;

    public CreateConsultationRequest() {
    }

    public CreateConsultationRequest(String notes, String treatment) {
        this(notes, treatment, null, null);
    }

    public CreateConsultationRequest(String notes, String treatment, LocalDate followUpDate, String followUpNotes) {
        this.notes = notes;
        this.treatment = treatment;
        this.followUpDate = followUpDate;
        this.followUpNotes = followUpNotes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getFollowUpNotes() {
        return followUpNotes;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }
}

