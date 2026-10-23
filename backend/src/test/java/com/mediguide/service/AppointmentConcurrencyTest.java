package com.mediguide.service;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.BookAppointmentRequest;
import com.mediguide.exception.AppointmentConflictException;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency integration tests verifying the dual-lock coordination strategy.
 * Demonstrates:
 * 1. Double-booking prevention for the same doctor across concurrent threads.
 * 2. Double-booking prevention for the same patient across different doctors.
 * 3. Race condition coordination between appointment booking and availability deactivation.
 * 4. Concurrent execution of valid adjacent appointments.
 */
@SpringBootTest
class AppointmentConcurrencyTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorAvailabilityService availabilityService;

    private User doctor1;
    private User doctor2;
    private User patient1;
    private User patient2;

    private UserPrincipal doctor1Principal;
    private UserPrincipal patient1Principal;
    private UserPrincipal patient2Principal;

    private DoctorAvailability availability1;
    private DoctorAvailability availability2;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        availabilityRepository.deleteAll();

        doctor1 = new User("Dr. Ramesh Sen", "concurrent.doc1." + System.nanoTime() + "@mediguide.local", "hash", Role.DOCTOR);
        userRepository.save(doctor1);
        doctor1Principal = new UserPrincipal(doctor1.getId(), doctor1.getEmail(), doctor1.getRole());

        doctor2 = new User("Dr. Aisha Sharma", "concurrent.doc2." + System.nanoTime() + "@mediguide.local", "hash", Role.DOCTOR);
        userRepository.save(doctor2);

        patient1 = new User("Anjali Gupta", "concurrent.pat1." + System.nanoTime() + "@mediguide.local", "hash", Role.PATIENT);
        userRepository.save(patient1);
        patient1Principal = new UserPrincipal(patient1.getId(), patient1.getEmail(), patient1.getRole());

        patient2 = new User("Vikram Verma", "concurrent.pat2." + System.nanoTime() + "@mediguide.local", "hash", Role.PATIENT);
        userRepository.save(patient2);
        patient2Principal = new UserPrincipal(patient2.getId(), patient2.getEmail(), patient2.getRole());

        slotStart = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        slotEnd = slotStart.plusHours(4);

        availability1 = availabilityRepository.save(new DoctorAvailability(doctor1, slotStart, slotEnd, true));
        availability2 = availabilityRepository.save(new DoctorAvailability(doctor2, slotStart, slotEnd, true));
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
        availabilityRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Concurrent bookings for same doctor with different patients: exactly 1 succeeds, 1 rejected")
    void shouldPreventDoubleBookingForSameDoctor() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        LocalDateTime apptStart = slotStart.plusHours(1);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Patient 1 books [10:00 - 10:30] with Doctor 1
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient1Principal,
                        new BookAppointmentRequest(availability1.getId(), apptStart, apptEnd));
                successCount.incrementAndGet();
            } catch (AppointmentConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        // Thread 2: Patient 2 books same [10:00 - 10:30] with Doctor 1
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient2Principal,
                        new BookAppointmentRequest(availability1.getId(), apptStart, apptEnd));
                successCount.incrementAndGet();
            } catch (AppointmentConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Threads should finish within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors: " + unexpectedErrors);

        assertEquals(1, successCount.get(), "Exactly one concurrent booking must succeed");
        assertEquals(1, conflictCount.get(), "The competing booking must be rejected with AppointmentConflictException");

        List<Appointment> bookedInDb = appointmentRepository.findByDoctorId(doctor1.getId());
        assertEquals(1, bookedInDb.size(), "Database must contain exactly 1 appointment for Doctor 1");
    }

    @Test
    @DisplayName("2. Concurrent bookings for same patient with different doctors: exactly 1 succeeds, 1 rejected")
    void shouldPreventDoubleBookingForSamePatientAcrossDifferentDoctors() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        LocalDateTime apptStart = slotStart.plusHours(1);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Patient 1 books [10:00 - 10:30] with Doctor 1
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient1Principal,
                        new BookAppointmentRequest(availability1.getId(), apptStart, apptEnd));
                successCount.incrementAndGet();
            } catch (AppointmentConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        // Thread 2: Patient 1 simultaneously books [10:00 - 10:30] with Doctor 2
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient1Principal,
                        new BookAppointmentRequest(availability2.getId(), apptStart, apptEnd));
                successCount.incrementAndGet();
            } catch (AppointmentConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Threads should finish within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors: " + unexpectedErrors);

        // Verification: Since patient row is locked in both transactions, one succeeds and the other sees overlap
        assertEquals(1, successCount.get(), "Exactly one concurrent booking for Patient 1 must succeed");
        assertEquals(1, conflictCount.get(), "The overlapping cross-doctor booking must fail with AppointmentConflictException");

        List<Appointment> patientAppts = appointmentRepository.findByPatientId(patient1.getId());
        assertEquals(1, patientAppts.size(), "Database must contain exactly 1 appointment for Patient 1");
    }

    @Test
    @DisplayName("3. Booking racing with availability deactivation: no appointment ever booked on deactivated slot")
    void shouldCoordinateBookingWithAvailabilityDeactivation() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        AtomicInteger bookingSuccess = new AtomicInteger(0);
        AtomicInteger bookingRejectedInactive = new AtomicInteger(0);
        AtomicInteger deactivationSuccess = new AtomicInteger(0);
        AtomicInteger deactivationRejectedHasBookings = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Patient books on availability1
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient1Principal,
                        new BookAppointmentRequest(availability1.getId(), apptStart, apptEnd));
                bookingSuccess.incrementAndGet();
            } catch (AppointmentValidationException e) {
                if (e.getMessage().contains("not active")) {
                    bookingRejectedInactive.incrementAndGet();
                } else {
                    unexpectedErrors.add(e);
                }
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        // Thread 2: Doctor deactivates availability1
        executor.submit(() -> {
            try {
                startGate.await();
                availabilityService.deactivateAvailability(doctor1Principal, availability1.getId());
                deactivationSuccess.incrementAndGet();
            } catch (AppointmentConflictException e) {
                if (e.getMessage().contains("future booked appointment")) {
                    deactivationRejectedHasBookings.incrementAndGet();
                } else {
                    unexpectedErrors.add(e);
                }
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Threads should finish within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors: " + unexpectedErrors);

        // State verification
        DoctorAvailability finalSlot = availabilityRepository.findById(availability1.getId()).orElseThrow();
        List<Appointment> bookedList = appointmentRepository.findByAvailabilityId(availability1.getId());

        // Invariant: Never allow an appointment to be booked against an inactive availability slot
        if (!finalSlot.isActive()) {
            assertEquals(1, deactivationSuccess.get(), "Deactivation succeeded");
            assertEquals(1, bookingRejectedInactive.get(), "Booking was rejected because slot was inactive");
            assertEquals(0, bookedList.size(), "No appointments should exist on a deactivated slot");
        } else {
            assertEquals(1, bookingSuccess.get(), "Booking succeeded first");
            assertEquals(1, deactivationRejectedHasBookings.get(), "Deactivation was rejected due to existing booking");
            assertEquals(1, bookedList.size(), "1 appointment booked on the active slot");
        }
    }

    @Test
    @DisplayName("4. Adjacent half-open appointments execute concurrently without false conflicts")
    void shouldAllowConcurrentAdjacentAppointments() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        LocalDateTime appt1Start = slotStart.plusHours(1);
        LocalDateTime appt1End = appt1Start.plusMinutes(30); // 10:00 - 10:30

        LocalDateTime appt2Start = appt1End; // 10:30 - 11:00 (adjacent)
        LocalDateTime appt2End = appt2Start.plusMinutes(30);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient1Principal,
                        new BookAppointmentRequest(availability1.getId(), appt1Start, appt1End));
                successCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.bookAppointment(patient2Principal,
                        new BookAppointmentRequest(availability1.getId(), appt2Start, appt2End));
                successCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Threads should finish within timeout");
        assertTrue(errors.isEmpty(), "Unexpected errors: " + errors);
        assertEquals(2, successCount.get(), "Both adjacent appointments must succeed");

        List<Appointment> bookedInDb = appointmentRepository.findByDoctorId(doctor1.getId());
        assertEquals(2, bookedInDb.size(), "Database must contain both adjacent appointments");
    }

    @Test
    @DisplayName("5. Concurrent cancellation and completion on same appointment: exactly one succeeds, other rejected with 409 Conflict")
    void shouldSerializeConcurrentCancellationAndCompletion() throws InterruptedException {
        // Book a valid appointment
        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);
        var booked = appointmentService.bookAppointment(patient1Principal,
                new BookAppointmentRequest(availability1.getId(), apptStart, apptEnd));
        Long appointmentId = booked.getId();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger cancellationSuccess = new AtomicInteger(0);
        AtomicInteger completionSuccess = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Doctor attempts to complete
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.completeAppointment(doctor1Principal, appointmentId);
                completionSuccess.incrementAndGet();
            } catch (AppointmentConflictException ace) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        // Thread 2: Patient attempts to cancel
        executor.submit(() -> {
            try {
                startGate.await();
                appointmentService.cancelAppointment(patient1Principal, appointmentId);
                cancellationSuccess.incrementAndGet();
            } catch (AppointmentConflictException ace) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                unexpectedErrors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Threads should finish within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors: " + unexpectedErrors);

        // Exactly one transition must commit, and the other must be rejected with 409 Conflict
        assertEquals(1, cancellationSuccess.get() + completionSuccess.get(),
                "Exactly one transition (cancel or complete) must commit");
        assertEquals(1, conflictCount.get(),
                "The losing concurrent thread must receive AppointmentConflictException");

        Appointment finalAppt = appointmentRepository.findById(appointmentId).orElseThrow();
        assertTrue(finalAppt.getStatus() == AppointmentStatus.CANCELLED ||
                   finalAppt.getStatus() == AppointmentStatus.COMPLETED,
                "Final status must be either CANCELLED or COMPLETED");
    }
}
