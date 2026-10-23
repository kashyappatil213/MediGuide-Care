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
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DoctorAvailabilityServiceTest {

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private DoctorAvailabilityService availabilityService;

    private Clock fixedClock;
    private LocalDateTime fixedNow;

    private User doctor;
    private User otherDoctor;
    private User patient;

    private UserPrincipal doctorPrincipal;
    private UserPrincipal otherDoctorPrincipal;
    private UserPrincipal patientPrincipal;

    @BeforeEach
    void setUp() {
        // Fixed clock for deterministic timestamp assertions: 2026-10-15T09:00:00
        ZoneId zone = ZoneId.of("UTC");
        fixedClock = Clock.fixed(Instant.parse("2026-10-15T09:00:00Z"), zone);
        fixedNow = LocalDateTime.now(fixedClock);

        availabilityService = new DoctorAvailabilityService(
                availabilityRepository,
                appointmentRepository,
                userRepository,
                fixedClock,
                entityManager
        );


        doctor = new User("Dr. Ramesh Sen", "doc.avail.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.DOCTOR);
        userRepository.save(doctor);
        doctorPrincipal = new UserPrincipal(doctor.getId(), doctor.getEmail(), doctor.getRole());

        otherDoctor = new User("Dr. Aisha Sharma", "other.doc.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.DOCTOR);
        userRepository.save(otherDoctor);
        otherDoctorPrincipal = new UserPrincipal(otherDoctor.getId(), otherDoctor.getEmail(), otherDoctor.getRole());

        patient = new User("Anjali Gupta", "patient.avail.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.PATIENT);
        userRepository.save(patient);
        patientPrincipal = new UserPrincipal(patient.getId(), patient.getEmail(), patient.getRole());
    }

    @Test
    @DisplayName("1. Doctor should successfully create an active availability interval")
    void shouldCreateAvailabilitySuccessfully() {
        LocalDateTime start = fixedNow.plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end, true);
        AvailabilityResponse response = availabilityService.createAvailability(doctorPrincipal, request);

        assertNotNull(response.getId());
        assertEquals(doctor.getId(), response.getDoctorId());
        assertEquals(doctor.getFullName(), response.getDoctorName());
        assertEquals(start, response.getStartTime());
        assertEquals(end, response.getEndTime());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("2. Should reject availability interval scheduled in the past")
    void shouldRejectAvailabilityInThePast() {
        LocalDateTime pastStart = fixedNow.minusDays(1);
        LocalDateTime pastEnd = pastStart.plusHours(2);

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(pastStart, pastEnd);

        AppointmentValidationException ex = assertThrows(AppointmentValidationException.class, () ->
                availabilityService.createAvailability(doctorPrincipal, request));
        assertTrue(ex.getMessage().contains("past"));
    }

    @Test
    @DisplayName("3. Should reject availability interval where end time is before start time")
    void shouldRejectAvailabilityWithInvertedTimes() {
        LocalDateTime start = fixedNow.plusDays(1).withHour(12).withMinute(0);
        LocalDateTime invalidEnd = start.minusHours(1);

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, invalidEnd);

        AppointmentValidationException ex = assertThrows(AppointmentValidationException.class, () ->
                availabilityService.createAvailability(doctorPrincipal, request));
        assertTrue(ex.getMessage().contains("strictly after start time"));
    }

    @Test
    @DisplayName("4. Non-doctor caller should be rejected when attempting to create availability")
    void shouldRejectNonDoctorFromCreatingAvailability() {
        LocalDateTime start = fixedNow.plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(3);

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end);

        assertThrows(AccessDeniedException.class, () ->
                availabilityService.createAvailability(patientPrincipal, request));
    }

    @Test
    @DisplayName("5. Doctor cannot create availability on behalf of another doctor")
    void shouldRejectDoctorCreatingAvailabilityForAnotherDoctor() {
        LocalDateTime start = fixedNow.plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(3);

        // doctorPrincipal attempts to specify otherDoctor.getId()
        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end, true, otherDoctor.getId());

        assertThrows(AccessDeniedException.class, () ->
                availabilityService.createAvailability(doctorPrincipal, request));
    }

    @Test
    @DisplayName("6. Doctor should successfully deactivate their own availability slot when no appointments exist")
    void shouldDeactivateAvailabilityWhenNoAppointments() {
        LocalDateTime start = fixedNow.plusDays(2).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor, start, end, true));

        AvailabilityResponse response = availabilityService.deactivateAvailability(doctorPrincipal, slot.getId());
        assertFalse(response.isActive());

        DoctorAvailability updated = availabilityRepository.findById(slot.getId()).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("7. Should reject deactivation when future BOOKED appointments depend on the slot")
    void shouldRejectDeactivationWhenFutureBookedAppointmentsDependOnSlot() {
        LocalDateTime start = fixedNow.plusDays(2).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor, start, end, true));

        LocalDateTime apptStart = start.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        appointmentRepository.save(new Appointment(patient, doctor, slot, apptStart, apptEnd, AppointmentStatus.BOOKED));

        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                availabilityService.deactivateAvailability(doctorPrincipal, slot.getId()));
        assertTrue(ex.getMessage().contains("future booked appointment"));
    }

    @Test
    @DisplayName("8. Doctor should be allowed to deactivate slot if dependent appointments are CANCELLED or COMPLETED")
    void shouldAllowDeactivationWhenDependentAppointmentsAreCancelledOrCompleted() {
        LocalDateTime start = fixedNow.plusDays(2).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor, start, end, true));

        LocalDateTime apptStart1 = start.plusMinutes(15);
        LocalDateTime apptEnd1 = apptStart1.plusMinutes(30);
        appointmentRepository.save(new Appointment(patient, doctor, slot, apptStart1, apptEnd1, AppointmentStatus.CANCELLED));

        LocalDateTime apptStart2 = start.plusHours(1);
        LocalDateTime apptEnd2 = apptStart2.plusMinutes(30);
        appointmentRepository.save(new Appointment(patient, doctor, slot, apptStart2, apptEnd2, AppointmentStatus.COMPLETED));

        AvailabilityResponse response = availabilityService.deactivateAvailability(doctorPrincipal, slot.getId());
        assertFalse(response.isActive());
    }

    @Test
    @DisplayName("9. Doctor cannot deactivate another doctor's availability slot")
    void shouldRejectDeactivatingAnotherDoctorsSlot() {
        LocalDateTime start = fixedNow.plusDays(2).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor, start, end, true));

        assertThrows(AccessDeniedException.class, () ->
                availabilityService.deactivateAvailability(otherDoctorPrincipal, slot.getId()));
    }

    @Test
    @DisplayName("10. Patient cannot deactivate a doctor's availability slot")
    void shouldRejectPatientFromDeactivatingAvailability() {
        LocalDateTime start = fixedNow.plusDays(2).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(4);

        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor, start, end, true));

        assertThrows(AccessDeniedException.class, () ->
                availabilityService.deactivateAvailability(patientPrincipal, slot.getId()));
    }

    @Test
    @DisplayName("11. Deactivating non-existent availability should throw ResourceNotFoundException")
    void shouldThrowNotFoundWhenDeactivatingNonExistentSlot() {
        assertThrows(ResourceNotFoundException.class, () ->
                availabilityService.deactivateAvailability(doctorPrincipal, 999999L));
    }

    @Test
    @DisplayName("12. Should query and list availability slots by doctor ID")
    void shouldListAvailabilitySlotsByDoctor() {
        LocalDateTime start1 = fixedNow.plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end1 = start1.plusHours(3);
        DoctorAvailability slot1 = availabilityRepository.save(new DoctorAvailability(doctor, start1, end1, true));

        LocalDateTime start2 = fixedNow.plusDays(2).withHour(14).withMinute(0);
        LocalDateTime end2 = start2.plusHours(3);
        DoctorAvailability slot2 = availabilityRepository.save(new DoctorAvailability(doctor, start2, end2, false));

        List<AvailabilityResponse> all = availabilityService.getAvailabilitiesByDoctor(doctor.getId());
        assertEquals(2, all.size());

        List<AvailabilityResponse> activeOnly = availabilityService.getActiveAvailabilitiesByDoctor(doctor.getId());
        assertEquals(1, activeOnly.size());
        assertEquals(slot1.getId(), activeOnly.get(0).getId());
    }

    @Test
    @DisplayName("13. Doctor should successfully pre-schedule multiple availability slots in a single batch")
    void shouldCreateMultipleAvailabilitySlotsSuccessfullyInBatch() {
        LocalDateTime day = fixedNow.plusDays(1);
        LocalDateTime slot1Start = day.withHour(9).withMinute(0);
        LocalDateTime slot1End = slot1Start.plusHours(2);

        LocalDateTime slot2Start = day.withHour(11).withMinute(30);
        LocalDateTime slot2End = slot2Start.plusHours(2);

        LocalDateTime slot3Start = day.withHour(14).withMinute(0);
        LocalDateTime slot3End = slot3Start.plusHours(3);

        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(slot1Start, slot1End, true),
                new CreateAvailabilityRequest(slot2Start, slot2End, true),
                new CreateAvailabilityRequest(slot3Start, slot3End, true)
        );

        CreateBatchAvailabilityRequest batchRequest = new CreateBatchAvailabilityRequest(slots);
        BatchAvailabilityResponse response = availabilityService.createBatchAvailability(doctorPrincipal, batchRequest);

        assertEquals(3, response.getTotalRequested());
        assertEquals(3, response.getTotalCreated());
        assertEquals(0, response.getTotalFailed());
        assertEquals(3, response.getSuccessfulSlots().size());
        assertTrue(response.getRejectedSlots().isEmpty());

        // Verify each slot was saved in repository
        List<DoctorAvailability> savedSlots = availabilityRepository.findByDoctorIdOrderByStartTimeAsc(doctor.getId());
        assertEquals(3, savedSlots.size());
        assertEquals(slot1Start, savedSlots.get(0).getStartTime());
        assertEquals(slot2Start, savedSlots.get(1).getStartTime());
        assertEquals(slot3Start, savedSlots.get(2).getStartTime());
    }

    @Test
    @DisplayName("14. Overlapping slots within the same batch should record overlap rejection for conflicting slot while valid slot succeeds")
    void shouldRejectConflictingSlotWithinSameBatchWhileSavingValidSlot() {
        LocalDateTime day = fixedNow.plusDays(1);
        LocalDateTime slot1Start = day.withHour(9).withMinute(0);
        LocalDateTime slot1End = slot1Start.plusHours(3); // 09:00 - 12:00

        // slot 2 overlaps with slot 1 (10:00 - 13:00)
        LocalDateTime slot2Start = day.withHour(10).withMinute(0);
        LocalDateTime slot2End = slot2Start.plusHours(3);

        // slot 3 is non-overlapping (14:00 - 17:00)
        LocalDateTime slot3Start = day.withHour(14).withMinute(0);
        LocalDateTime slot3End = slot3Start.plusHours(3);

        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(slot1Start, slot1End, true),
                new CreateAvailabilityRequest(slot2Start, slot2End, true),
                new CreateAvailabilityRequest(slot3Start, slot3End, true)
        );

        BatchAvailabilityResponse response = availabilityService.createBatchAvailability(
                doctorPrincipal, new CreateBatchAvailabilityRequest(slots));

        assertEquals(3, response.getTotalRequested());
        assertEquals(2, response.getTotalCreated());
        assertEquals(1, response.getTotalFailed());

        assertEquals(1, response.getRejectedSlots().size());
        BatchSlotError rejected = response.getRejectedSlots().get(0);
        assertEquals(1, rejected.getIndex());
        assertTrue(rejected.getReason().contains("Overlaps with another slot in this batch"));

        // Only 2 slots should be persisted in database
        List<DoctorAvailability> saved = availabilityRepository.findByDoctorIdOrderByStartTimeAsc(doctor.getId());
        assertEquals(2, saved.size());
    }

    @Test
    @DisplayName("15. Batch slot overlapping an existing active availability slot in DB should be rejected with conflict reason")
    void shouldRejectBatchSlotOverlappingExistingSlotInDb() {
        LocalDateTime day = fixedNow.plusDays(2);
        // Pre-existing active slot in database: 09:00 - 12:00
        DoctorAvailability existingSlot = availabilityRepository.save(new DoctorAvailability(
                doctor, day.withHour(9).withMinute(0), day.withHour(12).withMinute(0), true
        ));

        // Batch: Slot 1 overlaps with existing slot (11:00 - 13:00); Slot 2 is valid (14:00 - 17:00)
        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(day.withHour(11).withMinute(0), day.withHour(13).withMinute(0), true),
                new CreateAvailabilityRequest(day.withHour(14).withMinute(0), day.withHour(17).withMinute(0), true)
        );

        BatchAvailabilityResponse response = availabilityService.createBatchAvailability(
                doctorPrincipal, new CreateBatchAvailabilityRequest(slots));

        assertEquals(2, response.getTotalRequested());
        assertEquals(1, response.getTotalCreated());
        assertEquals(1, response.getTotalFailed());

        BatchSlotError rejected = response.getRejectedSlots().get(0);
        assertEquals(0, rejected.getIndex());
        assertTrue(rejected.getReason().contains("Overlaps with existing active availability slot #" + existingSlot.getId()));
    }

    @Test
    @DisplayName("16. Batch with inverted times and past slots should report partial failure with specific error reasons")
    void shouldReportPartialFailureForInvalidIntervalsInBatch() {
        LocalDateTime day = fixedNow.plusDays(1);

        List<CreateAvailabilityRequest> slots = List.of(
                // Valid slot (09:00 - 11:00)
                new CreateAvailabilityRequest(day.withHour(9).withMinute(0), day.withHour(11).withMinute(0), true),
                // Inverted slot: end before start (14:00 - 12:00)
                new CreateAvailabilityRequest(day.withHour(14).withMinute(0), day.withHour(12).withMinute(0), true),
                // Past slot
                new CreateAvailabilityRequest(fixedNow.minusDays(1).withHour(9).withMinute(0), fixedNow.minusDays(1).withHour(12).withMinute(0), true)
        );

        BatchAvailabilityResponse response = availabilityService.createBatchAvailability(
                doctorPrincipal, new CreateBatchAvailabilityRequest(slots));

        assertEquals(3, response.getTotalRequested());
        assertEquals(1, response.getTotalCreated());
        assertEquals(2, response.getTotalFailed());

        assertEquals(2, response.getRejectedSlots().size());
        assertTrue(response.getRejectedSlots().get(0).getReason().contains("strictly after start time"));
        assertTrue(response.getRejectedSlots().get(1).getReason().contains("past"));
    }

    @Test
    @DisplayName("17. Non-doctor caller should be rejected from creating batch availability")
    void shouldRejectNonDoctorFromBatchAvailabilityCreation() {
        LocalDateTime day = fixedNow.plusDays(1);
        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(day.withHour(9).withMinute(0), day.withHour(12).withMinute(0))
        );

        assertThrows(AccessDeniedException.class, () ->
                availabilityService.createBatchAvailability(patientPrincipal, new CreateBatchAvailabilityRequest(slots)));
    }

    @Test
    @DisplayName("18. Doctor cannot specify another doctor's ID in batch request")
    void shouldRejectDoctorCreatingBatchForAnotherDoctor() {
        LocalDateTime day = fixedNow.plusDays(1);
        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(day.withHour(9).withMinute(0), day.withHour(12).withMinute(0))
        );

        CreateBatchAvailabilityRequest request = new CreateBatchAvailabilityRequest(slots, otherDoctor.getId());

        assertThrows(AccessDeniedException.class, () ->
                availabilityService.createBatchAvailability(doctorPrincipal, request));
    }

    @Test
    @DisplayName("19. Empty slot list in batch request should throw AppointmentValidationException")
    void shouldRejectEmptySlotListInBatch() {
        CreateBatchAvailabilityRequest emptyRequest = new CreateBatchAvailabilityRequest(List.of());

        assertThrows(AppointmentValidationException.class, () ->
                availabilityService.createBatchAvailability(doctorPrincipal, emptyRequest));
    }

    @Test
    @DisplayName("20. Single slot creation should reject overlapping active availability slot with AppointmentConflictException")
    void shouldRejectSingleSlotCreationWhenOverlappingExistingActiveSlot() {
        LocalDateTime day = fixedNow.plusDays(2);
        availabilityRepository.save(new DoctorAvailability(
                doctor, day.withHour(9).withMinute(0), day.withHour(12).withMinute(0), true
        ));

        // Attempt single creation overlapping [10:00 - 13:00]
        CreateAvailabilityRequest request = new CreateAvailabilityRequest(
                day.withHour(10).withMinute(0), day.withHour(13).withMinute(0), true
        );

        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                availabilityService.createAvailability(doctorPrincipal, request));
        assertTrue(ex.getMessage().contains("overlapping active availability slot"));
    }
}
