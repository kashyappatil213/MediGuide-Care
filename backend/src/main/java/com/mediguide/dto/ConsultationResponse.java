package com.mediguide.dto;

import com.mediguide.model.Appointment;
import com.mediguide.model.ConsultationRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Public response DTO representing a consultation record,
 * including any recommended follow-up schedule and notes.
 */
public class ConsultationResponse {

    private Long id;
    private Long appointmentId;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private String notes;
    private String treatment;
    private LocalDate followUpDate;
    private String followUpNotes;
    private LocalDateTime createdAt;

    public ConsultationResponse() {
    }

    public ConsultationResponse(Long id, Long appointmentId, Long doctorId, String doctorName,
                                Long patientId, String patientName, String notes, String treatment,
                                LocalDateTime createdAt) {
        this(id, appointmentId, doctorId, doctorName, patientId, patientName, notes, treatment, null, null, createdAt);
    }

    public ConsultationResponse(Long id, Long appointmentId, Long doctorId, String doctorName,
                                Long patientId, String patientName, String notes, String treatment,
                                LocalDate followUpDate, String followUpNotes, LocalDateTime createdAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.notes = notes;
        this.treatment = treatment;
        this.followUpDate = followUpDate;
        this.followUpNotes = followUpNotes;
        this.createdAt = createdAt;
    }

    /**
     * Converts a ConsultationRecord JPA entity into a public response DTO.
     */
    public static ConsultationResponse fromEntity(ConsultationRecord record) {
        if (record == null) {
            return null;
        }
        Appointment app = record.getAppointment();
        return new ConsultationResponse(
                record.getId(),
                app != null ? app.getId() : null,
                app != null && app.getDoctor() != null ? app.getDoctor().getId() : null,
                app != null && app.getDoctor() != null ? app.getDoctor().getFullName() : null,
                app != null && app.getPatient() != null ? app.getPatient().getId() : null,
                app != null && app.getPatient() != null ? app.getPatient().getFullName() : null,
                record.getNotes(),
                record.getTreatment(),
                record.getFollowUpDate(),
                record.getFollowUpNotes(),
                record.getCreatedAt()
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

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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
}

