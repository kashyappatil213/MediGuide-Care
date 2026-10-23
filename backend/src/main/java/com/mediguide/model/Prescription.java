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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * JPA entity representing a prescription item prescribed during an appointment/consultation.
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Associated appointment is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private ConsultationRecord consultation;

    @NotBlank(message = "Medication name is required")
    @Size(max = 150, message = "Medication name must not exceed 150 characters")
    @Column(name = "medication_name", nullable = false, length = 150)
    private String medicationName;

    @NotBlank(message = "Dosage is required")
    @Size(max = 100, message = "Dosage must not exceed 100 characters")
    @Column(name = "dosage", nullable = false, length = 100)
    private String dosage;

    @NotBlank(message = "Instructions are required")
    @Size(max = 255, message = "Instructions must not exceed 255 characters")
    @Column(name = "instructions", nullable = false, length = 255)
    private String instructions;

    @Size(max = 100, message = "Duration must not exceed 100 characters")
    @Column(name = "duration", length = 100)
    private String duration;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Prescription() {
    }

    public Prescription(Appointment appointment, ConsultationRecord consultation,
                        String medicationName, String dosage, String instructions, String duration) {
        this.appointment = appointment;
        this.consultation = consultation;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.instructions = instructions;
        this.duration = duration;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public ConsultationRecord getConsultation() {
        return consultation;
    }

    public void setConsultation(ConsultationRecord consultation) {
        this.consultation = consultation;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Prescription{" +
                "id=" + id +
                ", appointmentId=" + (appointment != null ? appointment.getId() : null) +
                ", medicationName='" + medicationName + '\'' +
                ", dosage='" + dosage + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
