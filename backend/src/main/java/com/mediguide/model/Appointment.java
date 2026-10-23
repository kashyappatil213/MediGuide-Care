package com.mediguide.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA entity representing a patient consultation appointment with a doctor.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Patient is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @NotNull(message = "Doctor is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @NotNull(message = "Doctor availability reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "availability_id", nullable = false)
    private DoctorAvailability availability;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default no-argument constructor required by JPA.
     */
    public Appointment() {
    }

    /**
     * Parameterized constructor for creating appointments.
     *
     * @param patient patient user
     * @param doctor doctor user
     * @param availability doctor availability slot
     * @param startTime appointment start time
     * @param endTime appointment end time
     * @param status appointment status
     */
    public Appointment(User patient, User doctor, DoctorAvailability availability,
                       LocalDateTime startTime, LocalDateTime endTime, AppointmentStatus status) {
        this.patient = patient;
        this.doctor = doctor;
        this.availability = availability;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : AppointmentStatus.BOOKED;
    }

    /**
     * Convenience constructor defaulting status to BOOKED.
     */
    public Appointment(User patient, User doctor, DoctorAvailability availability,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this(patient, doctor, availability, startTime, endTime, AppointmentStatus.BOOKED);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AppointmentStatus.BOOKED;
        }
    }

    // Bean validation constraints

    /**
     * Ensures that the appointment end time is strictly after the start time.
     */
    @AssertTrue(message = "Appointment end time must be strictly after start time")
    public boolean isEndAfterStart() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }

    /**
     * Ensures that the patient and doctor are distinct users.
     */
    @AssertTrue(message = "Patient and doctor must be different users")
    public boolean isPatientDifferentFromDoctor() {
        if (patient == null || doctor == null) {
            return true;
        }
        if (patient.getId() != null && doctor.getId() != null) {
            return !patient.getId().equals(doctor.getId());
        }
        return patient != doctor;
    }

    /**
     * Ensures that the patient reference has the PATIENT role.
     */
    @AssertTrue(message = "Patient reference must have PATIENT role")
    public boolean isPatientRoleValid() {
        if (patient == null || patient.getRole() == null) {
            return true;
        }
        return patient.getRole() == Role.PATIENT;
    }

    /**
     * Ensures that the doctor reference has the DOCTOR role.
     */
    @AssertTrue(message = "Doctor reference must have DOCTOR role")
    public boolean isDoctorRoleValid() {
        if (doctor == null || doctor.getRole() == null) {
            return true;
        }
        return doctor.getRole() == Role.DOCTOR;
    }

    /**
     * Ensures that the appointment doctor matches the owner of the referenced availability slot.
     */
    @AssertTrue(message = "Appointment doctor must match availability owner")
    public boolean isDoctorMatchingAvailability() {
        if (doctor == null || availability == null || availability.getDoctor() == null) {
            return true;
        }
        if (doctor.getId() != null && availability.getDoctor().getId() != null) {
            return doctor.getId().equals(availability.getDoctor().getId());
        }
        return doctor.equals(availability.getDoctor());
    }

    /**
     * Ensures that the appointment interval falls entirely within the referenced availability interval.
     */
    @AssertTrue(message = "Appointment time must fall entirely within the referenced availability interval")
    public boolean isWithinAvailabilityInterval() {
        if (startTime == null || endTime == null || availability == null) {
            return true;
        }
        LocalDateTime slotStart = availability.getStartTime();
        LocalDateTime slotEnd = availability.getEndTime();
        if (slotStart == null || slotEnd == null) {
            return true;
        }
        return !startTime.isBefore(slotStart) && !endTime.isAfter(slotEnd);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
    }

    public DoctorAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(DoctorAvailability availability) {
        this.availability = availability;
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

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", patientId=" + (patient != null ? patient.getId() : null) +
                ", doctorId=" + (doctor != null ? doctor.getId() : null) +
                ", availabilityId=" + (availability != null ? availability.getId() : null) +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
