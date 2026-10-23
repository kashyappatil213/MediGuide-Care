package com.mediguide.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing a clinical consultation record associated with an appointment.
 * Captures the attending doctor's clinical notes, diagnosis, treatment recommendations,
 * and optional recommended follow-up schedule and notes.
 */
@Entity
@Table(name = "consultations")
public class ConsultationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Associated appointment is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @NotBlank(message = "Consultation notes or diagnosis are required")
    @Size(max = 2000, message = "Consultation notes must not exceed 2000 characters")
    @Column(name = "notes", nullable = false, length = 2000)
    private String notes;

    @Size(max = 2000, message = "Treatment advice must not exceed 2000 characters")
    @Column(name = "treatment", length = 2000)
    private String treatment;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Size(max = 1000, message = "Follow-up notes must not exceed 1000 characters")
    @Column(name = "follow_up_notes", length = 1000)
    private String followUpNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ConsultationRecord() {
    }

    public ConsultationRecord(Appointment appointment, String notes, String treatment) {
        this(appointment, notes, treatment, null, null);
    }

    public ConsultationRecord(Appointment appointment, String notes, String treatment,
                              LocalDate followUpDate, String followUpNotes) {
        this.appointment = appointment;
        this.notes = notes;
        this.treatment = treatment;
        this.followUpDate = followUpDate;
        this.followUpNotes = followUpNotes;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ConsultationRecord{" +
                "id=" + id +
                ", appointmentId=" + (appointment != null ? appointment.getId() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}
