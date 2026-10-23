package com.mediguide.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * JPA entity representing a doctor's availability interval for accepting clinic appointments.
 */
@Entity
@Table(name = "doctor_availabilities")
public class DoctorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Doctor is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default no-argument constructor required by JPA.
     */
    public DoctorAvailability() {
    }

    /**
     * Parameterized constructor for creating availability slots.
     *
     * @param doctor owning doctor user
     * @param startTime start of the availability interval
     * @param endTime end of the availability interval
     * @param active whether this slot is currently active
     */
    public DoctorAvailability(User doctor, LocalDateTime startTime, LocalDateTime endTime, boolean active) {
        this.doctor = doctor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
    }

    /**
     * Convenience constructor defaulting active to true.
     *
     * @param doctor owning doctor user
     * @param startTime start of the availability interval
     * @param endTime end of the availability interval
     */
    public DoctorAvailability(User doctor, LocalDateTime startTime, LocalDateTime endTime) {
        this(doctor, startTime, endTime, true);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Bean validation constraints

    /**
     * Ensures that the availability interval end time is strictly after the start time.
     */
    @AssertTrue(message = "Availability end time must be strictly after start time")
    public boolean isEndAfterStart() {
        if (startTime == null || endTime == null) {
            return true; // Let @NotNull handle null values
        }
        return endTime.isAfter(startTime);
    }

    /**
     * Ensures that the owning user has the DOCTOR role.
     */
    @AssertTrue(message = "Availability owner must have DOCTOR role")
    public boolean isDoctorRoleValid() {
        if (doctor == null || doctor.getRole() == null) {
            return true; // Let @NotNull handle null doctor
        }
        return doctor.getRole() == Role.DOCTOR;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
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

    @Override
    public String toString() {
        return "DoctorAvailability{" +
                "id=" + id +
                ", doctorId=" + (doctor != null ? doctor.getId() : null) +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
