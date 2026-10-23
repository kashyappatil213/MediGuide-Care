package com.mediguide.dto;

import com.mediguide.model.DoctorAvailability;

import java.time.LocalDateTime;

/**
 * Public response DTO representing a doctor availability time interval.
 */
public class AvailabilityResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean active;
    private LocalDateTime createdAt;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(Long id, Long doctorId, String doctorName, LocalDateTime startTime,
                                LocalDateTime endTime, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.createdAt = createdAt;
    }

    /**
     * Converts a DoctorAvailability JPA entity into a public DTO.
     */
    public static AvailabilityResponse fromEntity(DoctorAvailability availability) {
        if (availability == null) {
            return null;
        }
        return new AvailabilityResponse(
                availability.getId(),
                availability.getDoctor() != null ? availability.getDoctor().getId() : null,
                availability.getDoctor() != null ? availability.getDoctor().getFullName() : null,
                availability.getStartTime(),
                availability.getEndTime(),
                availability.isActive(),
                availability.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
