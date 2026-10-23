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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AppointmentServiceTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private AppointmentService appointmentService;

    private Clock fixedClock;
    private LocalDateTime fixedNow;

    private User doctor;
    private User otherDoctor;
    private User patient;
    private User otherPatient;
    private User admin;

    private UserPrincipal doctorPrincipal;
    private UserPrincipal patientPrincipal;
    private UserPrincipal otherPatientPrincipal;
    private UserPrincipal adminPrincipal;

    private DoctorAvailability availability;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("UTC");
        fixedClock = Clock.fixed(Instant.parse("2026-10-15T09:00:00Z"), zone);
        fixedNow = LocalDateTime.now(fixedClock);

        appointmentService = new AppointmentService(
                appointmentRepository,
                availabilityRepository,
                userRepository,
                fixedClock,
                entityManager
        );


        doctor = new User("Dr. Ramesh Sen", "doc.appt.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.DOCTOR);
        userRepository.save(doctor);
        doctorPrincipal = new UserPrincipal(doctor.getId(), doctor.getEmail(), doctor.getRole());

        otherDoctor = new User("Dr. Aisha Sharma", "other.doc.appt.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.DOCTOR);
        userRepository.save(otherDoctor);

        patient = new User("Anjali Gupta", "patient.appt.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.PATIENT);
        userRepository.save(patient);
        patientPrincipal = new UserPrincipal(patient.getId(), patient.getEmail(), patient.getRole());

        otherPatient = new User("Vikram Verma", "other.pat.appt.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.PATIENT);
        userRepository.save(otherPatient);
        otherPatientPrincipal = new UserPrincipal(otherPatient.getId(), otherPatient.getEmail(), otherPatient.getRole());

        admin = new User("System Admin", "admin.appt.srv." + System.nanoTime() + "@mediguide.local", "hash", Role.ADMIN);
        userRepository.save(admin);
        adminPrincipal = new UserPrincipal(admin.getId(), admin.getEmail(), admin.getRole());

        // Availability interval: tomorrow 09:00 - 13:00 (4 hours)
        slotStart = fixedNow.plusDays(1).withHour(9).withMinute(0);
        slotEnd = slotStart.plusHours(4);
        availability = availabilityRepository.save(new DoctorAvailability(doctor, slotStart, slotEnd, true));
    }

    @Test
    @DisplayName("1. Valid patient should successfully book an appointment within active doctor availability")
    void shouldBookAppointmentSuccessfully() {
        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);
        AppointmentResponse response = appointmentService.bookAppointment(patientPrincipal, request);

        assertNotNull(response.getId());
        assertEquals(patient.getId(), response.getPatientId());
        assertEquals(patient.getFullName(), response.getPatientName());
        assertEquals(doctor.getId(), response.getDoctorId());
        assertEquals(doctor.getFullName(), response.getDoctorName());
        assertEquals(availability.getId(), response.getAvailabilityId());
        assertEquals(apptStart, response.getStartTime());
        assertEquals(apptEnd, response.getEndTime());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("2. Should reject booking when availability slot is inactive")
    void shouldRejectBookingWhenAvailabilityIsInactive() {
        DoctorAvailability inactiveSlot = availabilityRepository.save(
                new DoctorAvailability(doctor, slotStart.plusDays(1), slotEnd.plusDays(1), false));

        BookAppointmentRequest request = new BookAppointmentRequest(
                inactiveSlot.getId(),
                inactiveSlot.getStartTime().plusMinutes(15),
                inactiveSlot.getStartTime().plusMinutes(45));

        AppointmentValidationException ex = assertThrows(AppointmentValidationException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request));
        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    @DisplayName("3. Should reject booking when availability slot ID does not exist")
    void shouldRejectBookingForNonExistentAvailability() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(999999L, start, end);

        assertThrows(ResourceNotFoundException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request));
    }

    @Test
    @DisplayName("4. Patient cannot book an appointment for another patient ID")
    void shouldRejectBookingOnBehalfOfAnotherPatient() {
        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        // patientPrincipal specifies otherPatient.getId()
        BookAppointmentRequest request = new BookAppointmentRequest(
                availability.getId(), apptStart, apptEnd, otherPatient.getId());

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request));
    }

    @Test
    @DisplayName("5. Non-patient callers cannot book an appointment")
    void shouldRejectNonPatientFromBooking() {
        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);

        // Doctor principal attempts booking
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.bookAppointment(doctorPrincipal, request));
    }

    @Test
    @DisplayName("6. Appointments in the past relative to Clock must be rejected")
    void shouldRejectPastAppointments() {
        LocalDateTime pastStart = fixedNow.minusHours(2);
        LocalDateTime pastEnd = pastStart.plusMinutes(30);

        DoctorAvailability pastSlot = availabilityRepository.save(
                new DoctorAvailability(doctor, pastStart.minusHours(1), pastEnd.plusHours(1), true));

        BookAppointmentRequest request = new BookAppointmentRequest(pastSlot.getId(), pastStart, pastEnd);

        AppointmentValidationException ex = assertThrows(AppointmentValidationException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request));
        assertTrue(ex.getMessage().contains("past"));
    }

    @Test
    @DisplayName("7. Invalid intervals (start >= end) must be rejected")
    void shouldRejectInvalidIntervals() {
        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime invalidEnd = apptStart.minusMinutes(10);

        BookAppointmentRequest request = new BookAppointmentRequest(availability.getId(), apptStart, invalidEnd);

        AppointmentValidationException ex = assertThrows(AppointmentValidationException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request));
        assertTrue(ex.getMessage().contains("strictly after start time"));
    }

    @Test
    @DisplayName("8. Appointments outside the doctor availability slot must be rejected")
    void shouldRejectAppointmentOutsideAvailabilityInterval() {
        // Starts 15 minutes before availability
        LocalDateTime earlyStart = slotStart.minusMinutes(15);
        LocalDateTime end = slotStart.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(availability.getId(), earlyStart, end);

        AppointmentValidationException ex = assertThrows(AppointmentValidationException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request));
        assertTrue(ex.getMessage().contains("falls outside"));
    }

    @Test
    @DisplayName("9. Overlapping appointments for same doctor must be rejected, even across different availability slots")
    void shouldRejectOverlappingAppointmentsForSameDoctorAcrossSlots() {
        // Second availability slot for the SAME doctor, overlapping in time or separate slot
        DoctorAvailability slot2 = availabilityRepository.save(
                new DoctorAvailability(doctor, slotStart, slotEnd, true));

        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(45);

        // First appointment booked via slot1
        BookAppointmentRequest request1 = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);
        appointmentService.bookAppointment(patientPrincipal, request1);

        // Second appointment booked via slot2 for otherPatient in overlapping time [apptStart+15, apptEnd+15]
        LocalDateTime overlapStart = apptStart.plusMinutes(15);
        LocalDateTime overlapEnd = overlapStart.plusMinutes(45);

        BookAppointmentRequest request2 = new BookAppointmentRequest(slot2.getId(), overlapStart, overlapEnd);

        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                appointmentService.bookAppointment(otherPatientPrincipal, request2));
        assertTrue(ex.getMessage().contains("already has an overlapping appointment"));
    }

    @Test
    @DisplayName("10. Overlapping appointments for same patient across different doctors must be rejected")
    void shouldRejectOverlappingAppointmentsForSamePatientAcrossDoctors() {
        // Second availability slot for OTHER doctor
        DoctorAvailability otherDoctorSlot = availabilityRepository.save(
                new DoctorAvailability(otherDoctor, slotStart, slotEnd, true));

        LocalDateTime apptStart = slotStart.plusHours(1);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        // Patient books with doctor 1
        BookAppointmentRequest request1 = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);
        appointmentService.bookAppointment(patientPrincipal, request1);

        // Same patient attempts booking with doctor 2 at overlapping time [apptStart+10, apptEnd+20]
        LocalDateTime overlapStart = apptStart.plusMinutes(10);
        LocalDateTime overlapEnd = overlapStart.plusMinutes(30);

        BookAppointmentRequest request2 = new BookAppointmentRequest(otherDoctorSlot.getId(), overlapStart, overlapEnd);

        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                appointmentService.bookAppointment(patientPrincipal, request2));
        assertTrue(ex.getMessage().contains("Patient already has an overlapping appointment"));
    }

    @Test
    @DisplayName("11. Adjacent half-open appointments [start, end) must be allowed without conflict")
    void shouldAllowAdjacentAppointments() {
        LocalDateTime appt1Start = slotStart.plusHours(1);
        LocalDateTime appt1End = appt1Start.plusMinutes(30); // 10:00 - 10:30

        LocalDateTime appt2Start = appt1End; // 10:30 - 11:00 (adjacent)
        LocalDateTime appt2End = appt2Start.plusMinutes(30);

        BookAppointmentRequest req1 = new BookAppointmentRequest(availability.getId(), appt1Start, appt1End);
        AppointmentResponse resp1 = appointmentService.bookAppointment(patientPrincipal, req1);
        assertNotNull(resp1.getId());

        BookAppointmentRequest req2 = new BookAppointmentRequest(availability.getId(), appt2Start, appt2End);
        AppointmentResponse resp2 = appointmentService.bookAppointment(otherPatientPrincipal, req2);
        assertNotNull(resp2.getId());
    }

    @Test
    @DisplayName("12. CANCELLED and COMPLETED appointments do not block new bookings for the same interval")
    void shouldNotBlockBookingsWhenExistingAppointmentsAreCancelledOrCompleted() {
        LocalDateTime apptStart = slotStart.plusHours(2);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        // Previous appointment cancelled
        appointmentRepository.save(new Appointment(
                otherPatient, doctor, availability, apptStart, apptEnd, AppointmentStatus.CANCELLED));

        // New patient books the exact same interval
        BookAppointmentRequest request = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);
        AppointmentResponse response = appointmentService.bookAppointment(patientPrincipal, request);

        assertNotNull(response.getId());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());
    }

    @Test
    @DisplayName("13. Patients can view their own appointments but not another patient's appointments")
    void shouldEnforcePatientAppointmentAccessBoundaries() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        BookAppointmentRequest req = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal, req);

        // Patient can view their own
        List<AppointmentResponse> myAppointments = appointmentService.getAppointmentsByPatient(patientPrincipal, patient.getId());
        assertEquals(1, myAppointments.size());

        AppointmentResponse single = appointmentService.getAppointmentById(patientPrincipal, booked.getId());
        assertEquals(booked.getId(), single.getId());

        // Patient cannot view other patient's appointments
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.getAppointmentsByPatient(patientPrincipal, otherPatient.getId()));

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.getAppointmentById(otherPatientPrincipal, booked.getId()));
    }

    @Test
    @DisplayName("14. Doctors can view their assigned appointments but not another doctor's appointments")
    void shouldEnforceDoctorAppointmentAccessBoundaries() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        BookAppointmentRequest req = new BookAppointmentRequest(availability.getId(), apptStart, apptEnd);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal, req);

        // Doctor can view their appointments
        List<AppointmentResponse> docAppointments = appointmentService.getAppointmentsByDoctor(doctorPrincipal, doctor.getId());
        assertEquals(1, docAppointments.size());

        // Other doctor cannot view
        UserPrincipal otherDocPrincipal = new UserPrincipal(otherDoctor.getId(), otherDoctor.getEmail(), Role.DOCTOR);
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.getAppointmentsByDoctor(otherDocPrincipal, doctor.getId()));

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.getAppointmentById(otherDocPrincipal, booked.getId()));
    }

    // ==================== CANCELLATION TESTS ====================

    @Test
    @DisplayName("15. Assigned patient can successfully cancel their booked appointment")
    void shouldAllowAssignedPatientToCancelAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        AppointmentResponse cancelled = appointmentService.cancelAppointment(patientPrincipal, booked.getId());

        assertNotNull(cancelled);
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        assertEquals(booked.getId(), cancelled.getId());

        // Verify row still exists in DB with CANCELLED status
        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.CANCELLED, persisted.getStatus());
    }

    @Test
    @DisplayName("16. Assigned doctor can successfully cancel a booked appointment")
    void shouldAllowAssignedDoctorToCancelAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        AppointmentResponse cancelled = appointmentService.cancelAppointment(doctorPrincipal, booked.getId());

        assertNotNull(cancelled);
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.CANCELLED, persisted.getStatus());
    }

    @Test
    @DisplayName("17. Unrelated patient cannot cancel an appointment: 403 Forbidden")
    void shouldRejectUnrelatedPatientFromCancellingAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.cancelAppointment(otherPatientPrincipal, booked.getId()));

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.BOOKED, persisted.getStatus());
    }

    @Test
    @DisplayName("18. Unrelated doctor cannot cancel an appointment: 403 Forbidden")
    void shouldRejectUnrelatedDoctorFromCancellingAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        UserPrincipal otherDocPrincipal = new UserPrincipal(otherDoctor.getId(), otherDoctor.getEmail(), Role.DOCTOR);
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.cancelAppointment(otherDocPrincipal, booked.getId()));

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.BOOKED, persisted.getStatus());
    }

    @Test
    @DisplayName("19. Admin cannot cancel an appointment: 403 Forbidden")
    void shouldRejectAdminFromCancellingAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.cancelAppointment(adminPrincipal, booked.getId()));

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.BOOKED, persisted.getStatus());
    }

    @Test
    @DisplayName("20. Already cancelled appointment cannot be cancelled again: 409 Conflict")
    void shouldRejectCancellingAlreadyCancelledAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        // First cancellation succeeds
        appointmentService.cancelAppointment(patientPrincipal, booked.getId());

        // Second cancellation attempt fails with conflict
        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                appointmentService.cancelAppointment(patientPrincipal, booked.getId()));
        assertTrue(ex.getMessage().contains("BOOKED"));
    }

    @Test
    @DisplayName("21. Completed appointment cannot be cancelled: 409 Conflict")
    void shouldRejectCancellingCompletedAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        // Complete the appointment first
        appointmentService.completeAppointment(doctorPrincipal, booked.getId());

        // Attempting to cancel completed appointment must fail
        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                appointmentService.cancelAppointment(patientPrincipal, booked.getId()));
        assertTrue(ex.getMessage().contains("BOOKED"));
    }

    @Test
    @DisplayName("22. Cancelling non-existent appointment throws ResourceNotFoundException: 404")
    void shouldThrowNotFoundWhenCancellingNonExistentAppointment() {
        assertThrows(ResourceNotFoundException.class, () ->
                appointmentService.cancelAppointment(patientPrincipal, 999999L));
    }

    @Test
    @DisplayName("23. Unauthenticated caller cannot cancel an appointment")
    void shouldRejectUnauthenticatedCancellation() {
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.cancelAppointment(null, 1L));
    }

    // ==================== COMPLETION TESTS ====================

    @Test
    @DisplayName("24. Assigned doctor can successfully complete a booked appointment")
    void shouldAllowAssignedDoctorToCompleteAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        AppointmentResponse completed = appointmentService.completeAppointment(doctorPrincipal, booked.getId());

        assertNotNull(completed);
        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
        assertEquals(booked.getId(), completed.getId());

        // Verify row preserved in DB with COMPLETED status
        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.COMPLETED, persisted.getStatus());
    }

    @Test
    @DisplayName("25. Patient cannot complete an appointment: 403 Forbidden")
    void shouldRejectPatientFromCompletingAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.completeAppointment(patientPrincipal, booked.getId()));

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.BOOKED, persisted.getStatus());
    }

    @Test
    @DisplayName("26. Unrelated doctor cannot complete an appointment: 403 Forbidden")
    void shouldRejectUnrelatedDoctorFromCompletingAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        UserPrincipal otherDocPrincipal = new UserPrincipal(otherDoctor.getId(), otherDoctor.getEmail(), Role.DOCTOR);
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.completeAppointment(otherDocPrincipal, booked.getId()));

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.BOOKED, persisted.getStatus());
    }

    @Test
    @DisplayName("27. Admin cannot complete an appointment: 403 Forbidden")
    void shouldRejectAdminFromCompletingAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        assertThrows(AccessDeniedException.class, () ->
                appointmentService.completeAppointment(adminPrincipal, booked.getId()));

        Appointment persisted = appointmentRepository.findById(booked.getId()).orElse(null);
        assertNotNull(persisted);
        assertEquals(AppointmentStatus.BOOKED, persisted.getStatus());
    }

    @Test
    @DisplayName("28. Cancelled appointment cannot be completed: 409 Conflict")
    void shouldRejectCompletingCancelledAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        // Cancel first
        appointmentService.cancelAppointment(patientPrincipal, booked.getId());

        // Complete attempt must fail with conflict
        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                appointmentService.completeAppointment(doctorPrincipal, booked.getId()));
        assertTrue(ex.getMessage().contains("BOOKED"));
    }

    @Test
    @DisplayName("29. Already completed appointment cannot be completed again: 409 Conflict")
    void shouldRejectCompletingAlreadyCompletedAppointment() {
        LocalDateTime apptStart = slotStart.plusMinutes(15);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        AppointmentResponse booked = appointmentService.bookAppointment(patientPrincipal,
                new BookAppointmentRequest(availability.getId(), apptStart, apptEnd));

        // First completion succeeds
        appointmentService.completeAppointment(doctorPrincipal, booked.getId());

        // Second completion attempt fails with conflict
        AppointmentConflictException ex = assertThrows(AppointmentConflictException.class, () ->
                appointmentService.completeAppointment(doctorPrincipal, booked.getId()));
        assertTrue(ex.getMessage().contains("BOOKED"));
    }

    @Test
    @DisplayName("30. Completing non-existent appointment throws ResourceNotFoundException: 404")
    void shouldThrowNotFoundWhenCompletingNonExistentAppointment() {
        assertThrows(ResourceNotFoundException.class, () ->
                appointmentService.completeAppointment(doctorPrincipal, 999999L));
    }

    @Test
    @DisplayName("31. Unauthenticated caller cannot complete an appointment")
    void shouldRejectUnauthenticatedCompletion() {
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.completeAppointment(null, 1L));
    }
}
