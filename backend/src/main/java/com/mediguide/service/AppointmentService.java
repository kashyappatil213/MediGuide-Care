package com.mediguide.service;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.AppointmentResponse;
import com.mediguide.dto.BookAppointmentRequest;
import com.mediguide.exception.AppointmentConflictException;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.exception.ResourceNotFoundException;
import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing appointment scheduling, validation, and double-booking conflict prevention.
 * Enforces pessimistic row-level locking across both patient and doctor in ascending ID order
 * to coordinate concurrent booking requests and eliminate double-booking race conditions and deadlocks.
 */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final EntityManager entityManager;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorAvailabilityRepository availabilityRepository,
                              UserRepository userRepository,
                              Clock clock,
                              EntityManager entityManager) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        this.entityManager = entityManager;
    }

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorAvailabilityRepository availabilityRepository,
                              UserRepository userRepository,
                              Clock clock) {
        this(appointmentRepository, availabilityRepository, userRepository, clock, null);
    }

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorAvailabilityRepository availabilityRepository,
                              UserRepository userRepository) {
        this(appointmentRepository, availabilityRepository, userRepository, Clock.systemDefaultZone(), null);
    }

    /**
     * Books a patient consultation appointment within a doctor's active availability slot.
     * Coordinates concurrent bookings for both patient and doctor by acquiring pessimistic write locks
     * on both User rows in strict ascending User ID order, eliminating double-booking races and deadlocks.
     * Re-reads the availability entity under the lock to prevent races with availability deactivation.
     *
     * @param principal authenticated patient user principal
     * @param request booking details (availabilityId, startTime, endTime)
     * @return booked appointment response DTO
     * @throws AccessDeniedException if caller is unauthenticated, not a PATIENT, or attempts impersonation
     * @throws AppointmentValidationException if times are invalid, in the past, outside availability, or slot is inactive
     * @throws AppointmentConflictException if an overlapping BOOKED appointment exists for the doctor or patient
     * @throws ResourceNotFoundException if availability or user entity is not found
     */
    @Transactional
    public AppointmentResponse bookAppointment(UserPrincipal principal, BookAppointmentRequest request) {
        // 1. Caller authentication & role validation
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to book an appointment");
        }
        if (principal.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Only patients can book consultation appointments");
        }
        if (request == null) {
            throw new AppointmentValidationException("Appointment booking request cannot be null");
        }
        if (request.getPatientId() != null && !request.getPatientId().equals(principal.getId())) {
            throw new AccessDeniedException("Patients can only book appointments for their own account");
        }

        // 2. Request interval validation
        Long availabilityId = request.getAvailabilityId();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        if (availabilityId == null) {
            throw new AppointmentValidationException("Doctor availability slot ID is required");
        }
        if (startTime == null || endTime == null) {
            throw new AppointmentValidationException("Appointment start time and end time are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new AppointmentValidationException("Appointment end time must be strictly after start time");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (startTime.isBefore(now)) {
            throw new AppointmentValidationException("Cannot book an appointment in the past");
        }

        // 3. Load doctor availability slot to identify the target doctor
        DoctorAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor availability slot not found with id: " + availabilityId));

        User doctor = availability.getDoctor();
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            throw new AppointmentValidationException("The selected availability slot does not belong to a valid doctor");
        }

        Long patientId = principal.getId();
        Long doctorId = doctor.getId();

        if (patientId.equals(doctorId)) {
            throw new AppointmentValidationException("Patient and doctor must be different users");
        }

        // 4. Concurrency Control: Acquire exclusive pessimistic write locks on BOTH patient and doctor rows
        // in ascending User ID order to guarantee deterministic serialization and prevent deadlocks.
        User lockedPatient;
        User lockedDoctor;
        if (patientId < doctorId) {
            lockedPatient = userRepository.findByIdForUpdate(patientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient user not found with id: " + patientId));
            lockedDoctor = userRepository.findByIdForUpdate(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + doctorId));
        } else {
            lockedDoctor = userRepository.findByIdForUpdate(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + doctorId));
            lockedPatient = userRepository.findByIdForUpdate(patientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient user not found with id: " + patientId));
        }

        if (!lockedPatient.isEmailVerified()) {
            throw new AppointmentValidationException("Email verification is required to book consultation appointments. Please verify your email address.");
        }

        if (lockedPatient.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("User account does not possess PATIENT role");
        }
        if (lockedDoctor.getRole() != Role.DOCTOR) {
            throw new AppointmentValidationException("The selected availability slot does not belong to a valid doctor");
        }

        // 5. Re-read / refresh availability under lock so a transaction waiting on the lock observes fresh committed state
        if (entityManager != null) {
            entityManager.refresh(availability);
        }

        if (!availability.isActive()) {
            throw new AppointmentValidationException("The selected doctor availability slot is not active");
        }

        // 6. Validate appointment time falls entirely within availability slot
        if (startTime.isBefore(availability.getStartTime()) || endTime.isAfter(availability.getEndTime())) {
            throw new AppointmentValidationException(String.format(
                    "Requested appointment interval [%s - %s] falls outside the doctor's availability interval [%s - %s]",
                    startTime, endTime, availability.getStartTime(), availability.getEndTime()));
        }

        // 7. Check for overlapping BOOKED appointments for the doctor under lock (half-open: start < existingEnd && end > existingStart)
        List<Appointment> doctorConflicts = appointmentRepository.findOverlappingBookedAppointmentsForDoctor(
                lockedDoctor.getId(), startTime, endTime);
        if (!doctorConflicts.isEmpty()) {
            throw new AppointmentConflictException(String.format(
                    "Doctor %s already has an overlapping appointment in the interval [%s - %s]",
                    lockedDoctor.getFullName(), startTime, endTime));
        }

        // 8. Check for overlapping BOOKED appointments for the patient across any doctor under lock
        List<Appointment> patientConflicts = appointmentRepository.findOverlappingBookedAppointmentsForPatient(
                lockedPatient.getId(), startTime, endTime);
        if (!patientConflicts.isEmpty()) {
            throw new AppointmentConflictException(String.format(
                    "Patient already has an overlapping appointment in the interval [%s - %s]",
                    startTime, endTime));
        }

        // 9. Persist appointment with BOOKED status
        Appointment appointment = new Appointment(lockedPatient, lockedDoctor, availability, startTime, endTime, AppointmentStatus.BOOKED);
        Appointment saved = appointmentRepository.save(appointment);

        return AppointmentResponse.fromEntity(saved);
    }


    /**
     * Retrieves an appointment by ID with role-based access checks.
     *
     * @param principal authenticated user principal
     * @param appointmentId appointment primary key
     * @return appointment response DTO
     * @throws AccessDeniedException if caller is not the appointment's patient, doctor, or an admin
     * @throws ResourceNotFoundException if appointment is not found
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UserPrincipal principal, Long appointmentId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to view appointment details");
        }
        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        Long callerId = principal.getId();
        boolean isPatient = principal.getRole() == Role.PATIENT && appointment.getPatient().getId().equals(callerId);
        boolean isDoctor = principal.getRole() == Role.DOCTOR && appointment.getDoctor().getId().equals(callerId);
        boolean isAdmin = principal.getRole() == Role.ADMIN;

        if (!isPatient && !isDoctor && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this appointment");
        }

        return AppointmentResponse.fromEntity(appointment);
    }

    /**
     * Retrieves all appointments for a patient with authorization check.
     *
     * @param principal authenticated user principal
     * @param patientId patient primary key
     * @return list of appointment responses
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(UserPrincipal principal, Long patientId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (patientId == null) {
            throw new AppointmentValidationException("Patient ID is required");
        }
        if (principal.getRole() == Role.PATIENT && !principal.getId().equals(patientId)) {
            throw new AccessDeniedException("Patients can only view their own appointments");
        }

        return appointmentRepository.findByPatientId(patientId).stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves all appointments for a doctor with authorization check.
     *
     * @param principal authenticated user principal
     * @param doctorId doctor primary key
     * @return list of appointment responses
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(UserPrincipal principal, Long doctorId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required");
        }
        if (doctorId == null) {
            throw new AppointmentValidationException("Doctor ID is required");
        }
        if (principal.getRole() == Role.DOCTOR && !principal.getId().equals(doctorId)) {
            throw new AccessDeniedException("Doctors can only view their own appointments");
        }

        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    /**
     * Cancels an appointment.
     * Enforces exclusive row-level pessimistic write locking on the appointment entity
     * to coordinate concurrent cancellation and completion operations.
     * Enforces that only the assigned patient or assigned doctor may cancel the appointment.
     * Rejects cancellation if current status is not BOOKED (e.g. already CANCELLED or COMPLETED).
     *
     * @param principal authenticated user principal
     * @param appointmentId appointment primary key
     * @return updated appointment response DTO with status CANCELLED
     * @throws AccessDeniedException if caller is unauthenticated, not the assigned patient or doctor, or an admin
     * @throws AppointmentValidationException if appointmentId is null
     * @throws ResourceNotFoundException if appointment is not found
     * @throws AppointmentConflictException if appointment status is not BOOKED
     */
    @Transactional
    public AppointmentResponse cancelAppointment(UserPrincipal principal, Long appointmentId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to cancel an appointment");
        }
        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }

        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        Long callerId = principal.getId();
        boolean isAssignedPatient = principal.getRole() == Role.PATIENT && appointment.getPatient().getId().equals(callerId);
        boolean isAssignedDoctor = principal.getRole() == Role.DOCTOR && appointment.getDoctor().getId().equals(callerId);

        if (!isAssignedPatient && !isAssignedDoctor) {
            throw new AccessDeniedException("Only the assigned patient or assigned doctor can cancel this appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new AppointmentConflictException(
                    "Cannot cancel appointment: only appointments with status BOOKED can be cancelled. Current status is " + appointment.getStatus()
            );
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);

        return AppointmentResponse.fromEntity(updated);
    }

    /**
     * Marks an appointment as completed.
     * Enforces exclusive row-level pessimistic write locking on the appointment entity
     * to coordinate concurrent cancellation and completion operations.
     * Enforces that only the assigned doctor may mark the appointment as completed.
     * Rejects completion if current status is not BOOKED (e.g. already CANCELLED or COMPLETED).
     *
     * @param principal authenticated user principal
     * @param appointmentId appointment primary key
     * @return updated appointment response DTO with status COMPLETED
     * @throws AccessDeniedException if caller is unauthenticated, not the assigned doctor, or an admin
     * @throws AppointmentValidationException if appointmentId is null
     * @throws ResourceNotFoundException if appointment is not found
     * @throws AppointmentConflictException if appointment status is not BOOKED
     */
    @Transactional
    public AppointmentResponse completeAppointment(UserPrincipal principal, Long appointmentId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to complete an appointment");
        }
        if (appointmentId == null) {
            throw new AppointmentValidationException("Appointment ID is required");
        }

        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        Long callerId = principal.getId();
        boolean isAssignedDoctor = principal.getRole() == Role.DOCTOR && appointment.getDoctor().getId().equals(callerId);

        if (!isAssignedDoctor) {
            throw new AccessDeniedException("Only the assigned doctor can mark this appointment as completed");
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new AppointmentConflictException(
                    "Cannot complete appointment: only appointments with status BOOKED can be completed. Current status is " + appointment.getStatus()
            );
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment updated = appointmentRepository.save(appointment);

        return AppointmentResponse.fromEntity(updated);
    }
}
