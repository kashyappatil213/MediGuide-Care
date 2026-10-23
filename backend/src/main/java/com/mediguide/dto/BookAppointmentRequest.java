package com.mediguide.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for scheduling a patient consultation appointment.
 */
public class BookAppointmentRequest {

    @NotNull(message = "Availability slot ID is required")
    private Long availabilityId;

    @NotNull(message = "Appointment start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "Appointment end time is required")
    private LocalDateTime endTime;

    /**
     * Optional patient ID. If provided, the service verifies that it strictly matches
     * the authenticated caller's identity to prevent booking on behalf of another patient.
     */
    private Long patientId;

    public BookAppointmentRequest() {
    }

    public BookAppointmentRequest(Long availabilityId, LocalDateTime startTime, LocalDateTime endTime) {
        this.availabilityId = availabilityId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public BookAppointmentRequest(Long availabilityId, LocalDateTime startTime, LocalDateTime endTime, Long patientId) {
        this.availabilityId = availabilityId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.patientId = patientId;
    }

    public Long getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Long availabilityId) {
        this.availabilityId = availabilityId;
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

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
}
