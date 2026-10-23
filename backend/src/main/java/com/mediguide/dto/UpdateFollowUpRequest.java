package com.mediguide.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for recording or updating the recommended follow-up date and notes
 * for an appointment's clinical consultation.
 */
public class UpdateFollowUpRequest {

    private LocalDate followUpDate;

    @Size(max = 1000, message = "Follow-up notes must not exceed 1000 characters")
    private String followUpNotes;

    public UpdateFollowUpRequest() {
    }

    public UpdateFollowUpRequest(LocalDate followUpDate, String followUpNotes) {
        this.followUpDate = followUpDate;
        this.followUpNotes = followUpNotes;
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
