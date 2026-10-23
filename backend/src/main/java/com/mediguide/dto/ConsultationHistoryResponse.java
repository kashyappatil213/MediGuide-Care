package com.mediguide.dto;

import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.ConsultationRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Public response DTO representing a historical consultation record.
 * Aggregates consultation notes, treatment recommendations, prescribed medications,
 * follow-up schedule, and associated appointment timing in a clean read-only shape.
 */
public class ConsultationHistoryResponse {

    private Long consultationId;
    private Long appointmentId;
    private LocalDateTime appointmentStartTime;
    private LocalDateTime appointmentEndTime;
    private AppointmentStatus appointmentStatus;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private String notes;
    private String treatment;
    private LocalDate followUpDate;
    private String followUpNotes;
    private LocalDateTime createdAt;
    private List<PrescriptionResponse> prescriptions = new ArrayList<>();

    public ConsultationHistoryResponse() {
    }

    public ConsultationHistoryResponse(Long consultationId, Long appointmentId,
                                       LocalDateTime appointmentStartTime, LocalDateTime appointmentEndTime,
                                       AppointmentStatus appointmentStatus, Long doctorId, String doctorName,
                                       Long patientId, String patientName, String notes, String treatment,
                                       LocalDate followUpDate, String followUpNotes, LocalDateTime createdAt,
                                       List<PrescriptionResponse> prescriptions) {
        this.consultationId = consultationId;
        this.appointmentId = appointmentId;
        this.appointmentStartTime = appointmentStartTime;
        this.appointmentEndTime = appointmentEndTime;
        this.appointmentStatus = appointmentStatus;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.notes = notes;
        this.treatment = treatment;
        this.followUpDate = followUpDate;
        this.followUpNotes = followUpNotes;
        this.createdAt = createdAt;
        this.prescriptions = prescriptions != null ? prescriptions : new ArrayList<>();
    }

    /**
     * Converts a ConsultationRecord JPA entity and associated prescriptions into a ConsultationHistoryResponse DTO.
     *
     * @param record consultation record entity
     * @param prescriptions prescribed medications for this consultation
     * @return ConsultationHistoryResponse DTO
     */
    public static ConsultationHistoryResponse fromEntity(ConsultationRecord record, List<PrescriptionResponse> prescriptions) {
        if (record == null) {
            return null;
        }
        Appointment app = record.getAppointment();
        return new ConsultationHistoryResponse(
                record.getId(),
                app != null ? app.getId() : null,
                app != null ? app.getStartTime() : null,
                app != null ? app.getEndTime() : null,
                app != null ? app.getStatus() : null,
                app != null && app.getDoctor() != null ? app.getDoctor().getId() : null,
                app != null && app.getDoctor() != null ? app.getDoctor().getFullName() : null,
                app != null && app.getPatient() != null ? app.getPatient().getId() : null,
                app != null && app.getPatient() != null ? app.getPatient().getFullName() : null,
                record.getNotes(),
                record.getTreatment(),
                record.getFollowUpDate(),
                record.getFollowUpNotes(),
                record.getCreatedAt(),
                prescriptions
        );
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDateTime getAppointmentStartTime() {
        return appointmentStartTime;
    }

    public void setAppointmentStartTime(LocalDateTime appointmentStartTime) {
        this.appointmentStartTime = appointmentStartTime;
    }

    public LocalDateTime getAppointmentEndTime() {
        return appointmentEndTime;
    }

    public void setAppointmentEndTime(LocalDateTime appointmentEndTime) {
        this.appointmentEndTime = appointmentEndTime;
    }

    public AppointmentStatus getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(AppointmentStatus appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
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

    public List<PrescriptionResponse> getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(List<PrescriptionResponse> prescriptions) {
        this.prescriptions = prescriptions != null ? prescriptions : new ArrayList<>();
    }
}
