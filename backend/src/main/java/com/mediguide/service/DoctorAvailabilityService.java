package com.mediguide.service;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.AvailabilityResponse;
import com.mediguide.dto.BatchAvailabilityResponse;
import com.mediguide.dto.BatchSlotError;
import com.mediguide.dto.CreateAvailabilityRequest;
import com.mediguide.dto.CreateBatchAvailabilityRequest;
import com.mediguide.exception.AppointmentConflictException;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.exception.ResourceNotFoundException;
import com.mediguide.model.Appointment;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing doctor availability schedules and intervals.
 * Enforces ownership authorization, interval integrity, and dependency checks on deactivation.
 * Coordinates with booking operations via pessimistic locking on the owning doctor's User row.
 */
@Service
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final EntityManager entityManager;

    @Autowired
    public DoctorAvailabilityService(DoctorAvailabilityRepository availabilityRepository,
                                   AppointmentRepository appointmentRepository,
                                   UserRepository userRepository,
                                   Clock clock,
                                   EntityManager entityManager) {
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        this.entityManager = entityManager;
    }

    public DoctorAvailabilityService(DoctorAvailabilityRepository availabilityRepository,
                                   AppointmentRepository appointmentRepository,
                                   UserRepository userRepository,
                                   Clock clock) {
        this(availabilityRepository, appointmentRepository, userRepository, clock, null);
    }

    public DoctorAvailabilityService(DoctorAvailabilityRepository availabilityRepository,
                                   AppointmentRepository appointmentRepository,
                                   UserRepository userRepository) {
        this(availabilityRepository, appointmentRepository, userRepository, Clock.systemDefaultZone(), null);
    }


    /**
     * Creates a new availability interval for the authenticated doctor.
     *
     * @param principal authenticated doctor user principal
     * @param request creation parameters (start, end, active flag)
     * @return created availability response DTO
     * @throws AccessDeniedException if caller is unauthenticated, not a DOCTOR, or attempts impersonation
     * @throws AppointmentValidationException if start/end times are missing, inverted, or in the past
     * @throws AppointmentConflictException if an active overlapping availability slot exists for the doctor
     * @throws ResourceNotFoundException if the doctor user record is not found
     */
    @Transactional
    public AvailabilityResponse createAvailability(UserPrincipal principal, CreateAvailabilityRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to create doctor availability");
        }
        if (principal.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can create availability schedules");
        }
        if (request == null) {
            throw new AppointmentValidationException("Availability creation request cannot be null");
        }
        if (request.getDoctorId() != null && !request.getDoctorId().equals(principal.getId())) {
            throw new AccessDeniedException("Doctors cannot create availability schedules for other doctors");
        }

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        if (startTime == null || endTime == null) {
            throw new AppointmentValidationException("Availability start time and end time are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new AppointmentValidationException("Availability end time must be strictly after start time");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (startTime.isBefore(now)) {
            throw new AppointmentValidationException("Cannot create an availability slot in the past");
        }

        User doctor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + principal.getId()));

        if (doctor.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("User account does not possess DOCTOR privileges");
        }

        List<DoctorAvailability> conflicts = availabilityRepository.findOverlappingActiveAvailabilities(
                principal.getId(), startTime, endTime);
        if (!conflicts.isEmpty()) {
            DoctorAvailability conflict = conflicts.get(0);
            throw new AppointmentConflictException(String.format(
                    "Doctor already has an overlapping active availability slot #%d in interval [%s - %s]",
                    conflict.getId(), conflict.getStartTime(), conflict.getEndTime()));
        }

        boolean active = request.getActive() != null ? request.getActive() : true;
        DoctorAvailability availability = new DoctorAvailability(doctor, startTime, endTime, active);
        DoctorAvailability saved = availabilityRepository.save(availability);

        return AvailabilityResponse.fromEntity(saved);
    }

    /**
     * Pre-schedules multiple availability slots for the authenticated doctor in a single batch operation.
     * Enforces individual slot validation, pessimistic concurrency locking, and overlap checks against
     * both existing active slots in the database and other slots in the current batch.
     * Supports defined partial success: valid non-conflicting slots are persisted and returned in
     * successfulSlots, while invalid or conflicting slots are recorded in rejectedSlots with specific reasons.
     *
     * @param principal authenticated doctor user principal
     * @param request batch creation parameters containing the list of slot requests
     * @return BatchAvailabilityResponse detailing successful and rejected slots with counts
     * @throws AccessDeniedException if caller is unauthenticated, not a DOCTOR, or attempts doctor impersonation
     * @throws AppointmentValidationException if the batch request is null or contains an empty slot list
     * @throws ResourceNotFoundException if the doctor user record is not found
     */
    @Transactional
    public BatchAvailabilityResponse createBatchAvailability(UserPrincipal principal, CreateBatchAvailabilityRequest request) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to create doctor availability");
        }
        if (principal.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can create availability schedules");
        }
        if (request == null) {
            throw new AppointmentValidationException("Batch availability request cannot be null");
        }
        if (request.getDoctorId() != null && !request.getDoctorId().equals(principal.getId())) {
            throw new AccessDeniedException("Doctors cannot create availability schedules for other doctors");
        }
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new AppointmentValidationException("At least one availability slot must be provided");
        }

        // Concurrency Control: Acquire exclusive pessimistic write lock on the doctor's User row
        userRepository.findByIdForUpdate(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + principal.getId()));

        User doctor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + principal.getId()));

        if (doctor.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("User account does not possess DOCTOR privileges");
        }

        List<AvailabilityResponse> successfulSlots = new ArrayList<>();
        List<BatchSlotError> rejectedSlots = new ArrayList<>();
        List<DoctorAvailability> acceptedInBatch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        List<CreateAvailabilityRequest> slotRequests = request.getSlots();
        for (int i = 0; i < slotRequests.size(); i++) {
            CreateAvailabilityRequest item = slotRequests.get(i);
            if (item == null) {
                rejectedSlots.add(new BatchSlotError(i, null, null, "Slot request details cannot be null"));
                continue;
            }

            if (item.getDoctorId() != null && !item.getDoctorId().equals(principal.getId())) {
                throw new AccessDeniedException("Doctors cannot create availability schedules for other doctors");
            }

            LocalDateTime startTime = item.getStartTime();
            LocalDateTime endTime = item.getEndTime();

            if (startTime == null || endTime == null) {
                rejectedSlots.add(new BatchSlotError(i, startTime, endTime, "Availability start time and end time are required"));
                continue;
            }
            if (!startTime.isBefore(endTime)) {
                rejectedSlots.add(new BatchSlotError(i, startTime, endTime, "Availability end time must be strictly after start time"));
                continue;
            }
            if (startTime.isBefore(now)) {
                rejectedSlots.add(new BatchSlotError(i, startTime, endTime, "Cannot create an availability slot in the past"));
                continue;
            }

            // Check 1: Overlap against already accepted slots in this batch
            boolean batchConflict = acceptedInBatch.stream().anyMatch(acc ->
                    startTime.isBefore(acc.getEndTime()) && endTime.isAfter(acc.getStartTime()));
            if (batchConflict) {
                rejectedSlots.add(new BatchSlotError(i, startTime, endTime,
                        "Overlaps with another slot in this batch"));
                continue;
            }

            // Check 2: Overlap against doctor's pre-existing active availability intervals in database
            List<DoctorAvailability> dbConflicts = availabilityRepository.findOverlappingActiveAvailabilities(
                    principal.getId(), startTime, endTime);
            List<DoctorAvailability> preExistingConflicts = dbConflicts.stream()
                    .filter(c -> acceptedInBatch.stream().noneMatch(acc -> acc.getId().equals(c.getId())))
                    .toList();
            if (!preExistingConflicts.isEmpty()) {
                DoctorAvailability conflict = preExistingConflicts.get(0);
                rejectedSlots.add(new BatchSlotError(i, startTime, endTime, String.format(
                        "Overlaps with existing active availability slot #%d [%s - %s]",
                        conflict.getId(), conflict.getStartTime(), conflict.getEndTime())));
                continue;
            }

            // All validations passed: persist slot
            boolean active = item.getActive() != null ? item.getActive() : true;
            DoctorAvailability availability = new DoctorAvailability(doctor, startTime, endTime, active);
            DoctorAvailability saved = availabilityRepository.save(availability);

            acceptedInBatch.add(saved);
            successfulSlots.add(AvailabilityResponse.fromEntity(saved));
        }

        int totalRequested = slotRequests.size();
        int totalCreated = successfulSlots.size();
        int totalFailed = rejectedSlots.size();

        return new BatchAvailabilityResponse(totalRequested, totalCreated, totalFailed, successfulSlots, rejectedSlots);
    }

    /**
     * Deactivates a doctor availability slot.
     * Rejects deactivation if any future BOOKED appointments depend on this slot.
     *
     * @param principal authenticated doctor user principal
     * @param availabilityId primary key of the availability slot to deactivate
     * @return updated availability response DTO with active=false
     * @throws AccessDeniedException if caller is not the owner of the availability slot
     * @throws ResourceNotFoundException if availability slot does not exist
     * @throws AppointmentConflictException if future active booked appointments exist for this slot
     */
    @Transactional
    public AvailabilityResponse deactivateAvailability(UserPrincipal principal, Long availabilityId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to deactivate availability");
        }
        if (principal.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors can deactivate availability schedules");
        }
        if (availabilityId == null) {
            throw new AppointmentValidationException("Availability slot ID is required");
        }

        DoctorAvailability slot = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor availability slot not found with id: " + availabilityId));

        if (slot.getDoctor() == null || !slot.getDoctor().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Doctors can only deactivate their own availability schedules");
        }

        // Concurrency Control: Acquire exclusive pessimistic write lock on the doctor's User row.
        // Serializes with concurrent booking transactions for this doctor.
        userRepository.findByIdForUpdate(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with id: " + principal.getId()));

        // Re-read / refresh availability state under the lock
        if (entityManager != null) {
            entityManager.refresh(slot);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> futureBooked = appointmentRepository.findFutureBookedAppointmentsByAvailabilityId(availabilityId, now);
        if (!futureBooked.isEmpty()) {
            throw new AppointmentConflictException("Cannot deactivate availability slot: " + futureBooked.size() +
                    " future booked appointment(s) depend on this schedule. Cancel or complete them first.");
        }

        slot.setActive(false);
        DoctorAvailability updated = availabilityRepository.save(slot);

        return AvailabilityResponse.fromEntity(updated);
    }


    /**
     * Retrieves all availability slots registered for a given doctor.
     *
     * @param doctorId doctor primary key
     * @return chronologically sorted list of availability responses
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailabilitiesByDoctor(Long doctorId) {
        if (doctorId == null) {
            throw new AppointmentValidationException("Doctor ID is required");
        }
        return availabilityRepository.findByDoctorIdOrderByStartTimeAsc(doctorId).stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves active availability slots registered for a given doctor.
     *
     * @param doctorId doctor primary key
     * @return chronologically sorted list of active availability responses
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getActiveAvailabilitiesByDoctor(Long doctorId) {
        if (doctorId == null) {
            throw new AppointmentValidationException("Doctor ID is required");
        }
        return availabilityRepository.findByDoctorIdAndActiveTrueOrderByStartTimeAsc(doctorId).stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single doctor availability slot by ID.
     *
     * @param availabilityId availability primary key
     * @return availability response DTO
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailabilityById(Long availabilityId) {
        if (availabilityId == null) {
            throw new AppointmentValidationException("Availability ID is required");
        }
        DoctorAvailability slot = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor availability slot not found with id: " + availabilityId));
        return AvailabilityResponse.fromEntity(slot);
    }

    /**
     * Retrieves all active future doctor availability slots eligible for booking across all doctors,
     * sorted chronologically by start time.
     * Excludes inactive slots and slots whose end time is in the past relative to the injected Clock.
     * Used by patients to discover booking slots.
     *
     * @return chronologically sorted list of active future availability responses
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getActiveFutureAvailabilities() {
        LocalDateTime now = LocalDateTime.now(clock);
        return availabilityRepository.findActiveFutureAvailabilities(now).stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();
    }
}
