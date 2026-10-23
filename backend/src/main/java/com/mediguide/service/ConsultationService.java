package com.mediguide.service;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ConsultationHistoryResponse;
import com.mediguide.dto.ConsultationResponse;
import com.mediguide.dto.CreateConsultationRequest;
import com.mediguide.dto.CreatePrescriptionRequest;
import com.mediguide.dto.PrescriptionResponse;
import com.mediguide.dto.UpdateFollowUpRequest;
import com.mediguide.exception.AppointmentConflictException;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.exception.ResourceNotFoundException;
import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.ConsultationRecord;
import com.mediguide.model.Prescription;
import com.mediguide.model.Role;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.ConsultationRecordRepository;
import com.mediguide.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing clinical consultation records, follow-up recommendations, and medical prescriptions.
 * Enforces doctor ownership, patient participant verification, and lifecycle state constraints.
 */
@Service
public class ConsultationService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRecordRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final Clock clock;

    @Autowired
    public ConsultationService(AppointmentRepository appointmentRepository,
                               ConsultationRecordRepository consultationRepository,
                               PrescriptionRepository prescriptionRepository,
                               Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public ConsultationService(AppointmentRepository appointmentRepository,
                               ConsultationRecordRepository consultationRepository,
                               PrescriptionRepository prescriptionRepository) {
        this(appointmentRepository, consultationRepository, prescriptionRepository, Clock.systemDefaultZone());
    }

    /**
     * Creates a new consultation record for an appointment.
     * Permitted only for the doctor assigned to the appointment.
     *
     * @param principal authenticated user principal (must be assigned DOCTOR)
     * @param appointmentId target appointment ID
     * @param request clinical notes, treatment recommendations, and optional follow-up
     * @return ConsultationResponse
     */
    @Transactional
    public ConsultationResponse createConsultation(UserPrincipal principal, Long appointmentId,
                                                   CreateConsultationRequest request) {
        validateDoctorPrincipal(principal);

        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }
        if (request == null || request.getNotes() == null || request.getNotes().trim().isEmpty()) {
            throw new AppointmentValidationException("Consultation notes or diagnosis are required");
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateDoctorOwnership(principal, appointment);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentConflictException("Cannot create consultation record for a cancelled appointment");
        }

        if (consultationRepository.existsByAppointmentId(appointmentId)) {
            throw new AppointmentConflictException("Consultation record already exists for appointment with id: " + appointmentId);
        }

        validateFollowUp(request.getFollowUpDate(), request.getFollowUpNotes());

        String cleanedFollowUpNotes = request.getFollowUpNotes() != null && !request.getFollowUpNotes().trim().isEmpty()
                ? request.getFollowUpNotes().trim()
                : null;

        ConsultationRecord record = new ConsultationRecord(
                appointment,
                request.getNotes().trim(),
                request.getTreatment() != null ? request.getTreatment().trim() : null,
                request.getFollowUpDate(),
                cleanedFollowUpNotes
        );

        record = consultationRepository.save(record);
        return ConsultationResponse.fromEntity(record);
    }

    /**
     * Updates an existing consultation record for an appointment.
     * Permitted only for the doctor assigned to the appointment.
     *
     * @param principal authenticated user principal (must be assigned DOCTOR)
     * @param appointmentId target appointment ID
     * @param request updated clinical notes, treatment recommendations, and optional follow-up
     * @return ConsultationResponse
     */
    @Transactional
    public ConsultationResponse updateConsultation(UserPrincipal principal, Long appointmentId,
                                                   CreateConsultationRequest request) {
        validateDoctorPrincipal(principal);

        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }
        if (request == null || request.getNotes() == null || request.getNotes().trim().isEmpty()) {
            throw new AppointmentValidationException("Consultation notes or diagnosis are required");
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateDoctorOwnership(principal, appointment);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentConflictException("Cannot update consultation record for a cancelled appointment");
        }

        validateFollowUp(request.getFollowUpDate(), request.getFollowUpNotes());

        ConsultationRecord record = consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation record not found for appointment: " + appointmentId));

        record.setNotes(request.getNotes().trim());
        record.setTreatment(request.getTreatment() != null ? request.getTreatment().trim() : null);
        record.setFollowUpDate(request.getFollowUpDate());
        record.setFollowUpNotes(request.getFollowUpNotes() != null && !request.getFollowUpNotes().trim().isEmpty()
                ? request.getFollowUpNotes().trim()
                : null);

        record = consultationRepository.save(record);
        return ConsultationResponse.fromEntity(record);
    }

    /**
     * Updates the recommended follow-up date and notes for an appointment's consultation.
     * Permitted only for the doctor assigned to the appointment.
     *
     * @param principal authenticated user principal (must be assigned DOCTOR)
     * @param appointmentId target appointment ID
     * @param request follow-up date and optional notes
     * @return ConsultationResponse
     */
    @Transactional
    public ConsultationResponse updateFollowUp(UserPrincipal principal, Long appointmentId,
                                              UpdateFollowUpRequest request) {
        validateDoctorPrincipal(principal);

        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }
        if (request == null) {
            throw new AppointmentValidationException("Follow-up request cannot be null");
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateDoctorOwnership(principal, appointment);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentConflictException("Cannot schedule follow-up for a cancelled appointment");
        }

        validateFollowUp(request.getFollowUpDate(), request.getFollowUpNotes());

        ConsultationRecord record = consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation record not found for appointment: " + appointmentId));

        record.setFollowUpDate(request.getFollowUpDate());
        record.setFollowUpNotes(request.getFollowUpNotes() != null && !request.getFollowUpNotes().trim().isEmpty()
                ? request.getFollowUpNotes().trim()
                : null);

        record = consultationRepository.save(record);
        return ConsultationResponse.fromEntity(record);
    }

    private void validateFollowUp(LocalDate followUpDate, String followUpNotes) {
        if (followUpDate != null) {
            LocalDate today = LocalDate.now(clock);
            if (followUpDate.isBefore(today)) {
                throw new AppointmentValidationException("Recommended follow-up date cannot be in the past");
            }
        }
        if (followUpNotes != null && followUpNotes.length() > 1000) {
            throw new AppointmentValidationException("Follow-up notes must not exceed 1000 characters");
        }
    }

    /**
     * Retrieves the consultation record for an appointment.
     * Accessible by the assigned doctor, the booked patient, or an admin.
     *
     * @param principal authenticated user principal
     * @param appointmentId target appointment ID
     * @return ConsultationResponse
     */
    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationByAppointmentId(UserPrincipal principal, Long appointmentId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view consultation records");
        }
        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateParticipantAccess(principal, appointment);

        ConsultationRecord record = consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation record not found for appointment: " + appointmentId));

        return ConsultationResponse.fromEntity(record);
    }

    /**
     * Creates a new prescription item for an appointment.
     * Permitted only for the doctor assigned to the appointment.
     *
     * @param principal authenticated user principal (must be assigned DOCTOR)
     * @param appointmentId target appointment ID
     * @param request prescription details (medication, dosage, instructions, duration)
     * @return PrescriptionResponse
     */
    @Transactional
    public PrescriptionResponse createPrescription(UserPrincipal principal, Long appointmentId,
                                                   CreatePrescriptionRequest request) {
        validateDoctorPrincipal(principal);

        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }
        if (request == null) {
            throw new AppointmentValidationException("Prescription request cannot be null");
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateDoctorOwnership(principal, appointment);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentConflictException("Cannot create prescription for a cancelled appointment");
        }

        // Link with existing consultation if one has been recorded
        ConsultationRecord consultation = consultationRepository.findByAppointmentId(appointmentId).orElse(null);

        Prescription prescription = new Prescription(
                appointment,
                consultation,
                request.getMedicationName() != null ? request.getMedicationName().trim() : null,
                request.getDosage() != null ? request.getDosage().trim() : null,
                request.getInstructions() != null ? request.getInstructions().trim() : null,
                request.getDuration() != null ? request.getDuration().trim() : null
        );

        prescription = prescriptionRepository.save(prescription);
        return PrescriptionResponse.fromEntity(prescription);
    }

    /**
     * Retrieves all prescription items associated with an appointment.
     * Accessible by the assigned doctor, the booked patient, or an admin.
     *
     * @param principal authenticated user principal
     * @param appointmentId target appointment ID
     * @return list of PrescriptionResponse objects
     */
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsByAppointmentId(UserPrincipal principal, Long appointmentId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view prescriptions");
        }
        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        validateParticipantAccess(principal, appointment);

        List<Prescription> prescriptions = prescriptionRepository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId);
        return prescriptions.stream()
                .map(PrescriptionResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves the chronological consultation history for the authenticated patient.
     * Accessible only by the patient principal for their own records (or admin).
     *
     * @param principal authenticated user principal (must be PATIENT)
     * @return list of ConsultationHistoryResponse, newest first
     */
    @Transactional(readOnly = true)
    public List<ConsultationHistoryResponse> getPatientConsultationHistory(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view consultation history");
        }
        if (principal.getRole() != Role.PATIENT && principal.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only patients can retrieve patient consultation history");
        }

        List<ConsultationRecord> records = consultationRepository.findHistoryByPatientId(principal.getId());
        return mapToHistoryResponses(records);
    }

    /**
     * Retrieves the chronological consultation history conducted by the authenticated doctor.
     * Accessible only by the doctor principal (or admin).
     *
     * @param principal authenticated user principal (must be DOCTOR)
     * @return list of ConsultationHistoryResponse, newest first
     */
    @Transactional(readOnly = true)
    public List<ConsultationHistoryResponse> getDoctorConsultationHistory(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view consultation history");
        }
        if (principal.getRole() != Role.DOCTOR && principal.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only doctors can retrieve doctor consultation history");
        }

        List<ConsultationRecord> records = consultationRepository.findHistoryByDoctorId(principal.getId());
        return mapToHistoryResponses(records);
    }

    /**
     * Retrieves the clinical consultation history between a doctor and a specific patient.
     * Accessible by the assigned doctor who has consulted this patient, or the patient themselves.
     *
     * @param principal authenticated user principal
     * @param patientId target patient ID
     * @return list of ConsultationHistoryResponse, newest first
     */
    @Transactional(readOnly = true)
    public List<ConsultationHistoryResponse> getPatientHistoryForDoctor(UserPrincipal principal, Long patientId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view consultation history");
        }
        if (patientId == null) {
            throw new AppointmentValidationException("Patient ID is required");
        }

        if (principal.getRole() == Role.PATIENT) {
            if (!principal.getId().equals(patientId)) {
                throw new AccessDeniedException("Access denied: you cannot view another patient's clinical history");
            }
            return getPatientConsultationHistory(principal);
        }

        if (principal.getRole() == Role.DOCTOR) {
            boolean hasRelationship = appointmentRepository.existsByDoctorIdAndPatientId(principal.getId(), patientId);
            if (!hasRelationship) {
                throw new AccessDeniedException("Access denied: you are not an authorized doctor for this patient");
            }

            List<ConsultationRecord> records = consultationRepository.findHistoryByDoctorIdAndPatientId(principal.getId(), patientId);
            return mapToHistoryResponses(records);
        }

        if (principal.getRole() == Role.ADMIN) {
            List<ConsultationRecord> records = consultationRepository.findHistoryByPatientId(patientId);
            return mapToHistoryResponses(records);
        }

        throw new AccessDeniedException("Access denied: unsupported role for clinical record access");
    }

    private List<ConsultationHistoryResponse> mapToHistoryResponses(List<ConsultationRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> appointmentIds = records.stream()
                .map(c -> c.getAppointment() != null ? c.getAppointment().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<PrescriptionResponse>> prescriptionsByAppId = Collections.emptyMap();
        if (!appointmentIds.isEmpty()) {
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentIdInOrderByCreatedAtAsc(appointmentIds);
            prescriptionsByAppId = prescriptions.stream()
                    .map(PrescriptionResponse::fromEntity)
                    .collect(Collectors.groupingBy(PrescriptionResponse::getAppointmentId));
        }

        final Map<Long, List<PrescriptionResponse>> finalPrescriptions = prescriptionsByAppId;
        return records.stream()
                .map(r -> {
                    Long appId = r.getAppointment() != null ? r.getAppointment().getId() : null;
                    List<PrescriptionResponse> rxList = appId != null
                            ? finalPrescriptions.getOrDefault(appId, Collections.emptyList())
                            : Collections.emptyList();
                    return ConsultationHistoryResponse.fromEntity(r, rxList);
                })
                .toList();
    }

    // --- Helper Validation Methods ---

    private void validateDoctorPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (principal.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can perform this clinical operation");
        }
    }

    private Appointment findAppointmentOrThrow(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
    }

    private void validateDoctorOwnership(UserPrincipal principal, Appointment appointment) {
        if (appointment.getDoctor() == null || !appointment.getDoctor().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the assigned doctor may manage clinical records for this appointment");
        }
    }

    private void validateParticipantAccess(UserPrincipal principal, Appointment appointment) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        if (principal.getRole() == Role.PATIENT) {
            if (appointment.getPatient() == null || !appointment.getPatient().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: you are not the patient for this appointment");
            }
            return;
        }

        if (principal.getRole() == Role.DOCTOR) {
            if (appointment.getDoctor() == null || !appointment.getDoctor().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: you are not the assigned doctor for this appointment");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: unsupported role for clinical record access");
    }
}
