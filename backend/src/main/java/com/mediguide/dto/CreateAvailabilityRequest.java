package com.mediguide.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a doctor availability time slot.
 */
public class CreateAvailabilityRequest {

    @NotNull(message = "Availability start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "Availability end time is required")
    private LocalDateTime endTime;

    private Boolean active = true;

    /**
     * Optional doctor ID. If provided, the service verifies that it strictly matches
     * the authenticated doctor's identity to prevent slot creation on behalf of other doctors.
     */
    private Long doctorId;

    public CreateAvailabilityRequest() {
    }

    public CreateAvailabilityRequest(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = true;
    }

    public CreateAvailabilityRequest(LocalDateTime startTime, LocalDateTime endTime, Boolean active) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active != null ? active : true;
    }

    public CreateAvailabilityRequest(LocalDateTime startTime, LocalDateTime endTime, Boolean active, Long doctorId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active != null ? active : true;
        this.doctorId = doctorId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }
}
