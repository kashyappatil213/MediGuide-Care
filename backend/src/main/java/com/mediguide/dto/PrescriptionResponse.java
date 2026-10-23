package com.mediguide.dto;

import com.mediguide.model.Prescription;

import java.time.LocalDateTime;

/**
 * Public response DTO representing a prescribed medication.
 */
public class PrescriptionResponse {

    private Long id;
    private Long appointmentId;
    private Long consultationId;
    private String medicationName;
    private String dosage;
    private String instructions;
    private String duration;
    private LocalDateTime createdAt;

    public PrescriptionResponse() {
    }

    public PrescriptionResponse(Long id, Long appointmentId, Long consultationId,
                                String medicationName, String dosage, String instructions,
                                String duration, LocalDateTime createdAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.consultationId = consultationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.instructions = instructions;
        this.duration = duration;
        this.createdAt = createdAt;
    }

    /**
     * Converts a Prescription JPA entity into a public response DTO.
     */
    public static PrescriptionResponse fromEntity(Prescription p) {
        if (p == null) {
            return null;
        }
        return new PrescriptionResponse(
                p.getId(),
                p.getAppointment() != null ? p.getAppointment().getId() : null,
                p.getConsultation() != null ? p.getConsultation().getId() : null,
                p.getMedicationName(),
                p.getDosage(),
                p.getInstructions(),
                p.getDuration(),
                p.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
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
}
