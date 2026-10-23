package com.mediguide.repository;

import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DoctorAvailabilityRepositoryTest {

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    private User doctor;
    private User patient;

    @BeforeEach
    void setUp() {
        doctor = new User("Dr. Ramesh Sen", "doctor.avail." + System.nanoTime() + "@mediguide.local", "hash123", Role.DOCTOR);
        userRepository.save(doctor);

        patient = new User("Anjali Gupta", "patient.avail." + System.nanoTime() + "@mediguide.local", "hash123", Role.PATIENT);
        userRepository.save(patient);
    }

    @Test
    @DisplayName("1. Should persist DoctorAvailability and retrieve by ID and doctor")
    void shouldSaveAndRetrieveAvailability() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(3);

        DoctorAvailability slot = new DoctorAvailability(doctor, start, end, true);
        DoctorAvailability saved = availabilityRepository.save(slot);

        assertNotNull(saved.getId());
        assertEquals(doctor.getId(), saved.getDoctor().getId());
        assertEquals(start, saved.getStartTime());
        assertEquals(end, saved.getEndTime());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());

        Optional<DoctorAvailability> found = availabilityRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(doctor.getId(), found.get().getDoctor().getId());
    }

    @Test
    @DisplayName("2. Should query availabilities by doctor entity and doctor ID")
    void shouldQueryByDoctorAndDoctorId() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2);
        DoctorAvailability slot1 = new DoctorAvailability(doctor, baseTime.withHour(9).withMinute(0), baseTime.withHour(12).withMinute(0), true);
        DoctorAvailability slot2 = new DoctorAvailability(doctor, baseTime.withHour(14).withMinute(0), baseTime.withHour(17).withMinute(0), false);

        availabilityRepository.save(slot1);
        availabilityRepository.save(slot2);

        List<DoctorAvailability> allByDoctor = availabilityRepository.findByDoctor(doctor);
        assertEquals(2, allByDoctor.size());

        List<DoctorAvailability> allByDoctorId = availabilityRepository.findByDoctorId(doctor.getId());
        assertEquals(2, allByDoctorId.size());
    }

    @Test
    @DisplayName("3. Should filter active availability slots")
    void shouldFilterActiveAvailabilities() {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3);
        DoctorAvailability activeSlot = new DoctorAvailability(doctor, baseTime.withHour(10).withMinute(0), baseTime.withHour(13).withMinute(0), true);
        DoctorAvailability inactiveSlot = new DoctorAvailability(doctor, baseTime.withHour(14).withMinute(0), baseTime.withHour(16).withMinute(0), false);

        availabilityRepository.save(activeSlot);
        availabilityRepository.save(inactiveSlot);

        List<DoctorAvailability> activeList = availabilityRepository.findByDoctorIdAndActiveTrue(doctor.getId());
        assertEquals(1, activeList.size());
        assertEquals(activeSlot.getId(), activeList.get(0).getId());
        assertTrue(activeList.get(0).isActive());

        List<DoctorAvailability> allActive = availabilityRepository.findByActiveTrue();
        assertFalse(allActive.isEmpty());
    }

    @Test
    @DisplayName("4. Should find upcoming slots starting after a specific timestamp")
    void shouldFilterByStartTimeAfter() {
        LocalDateTime today = LocalDateTime.now();
        DoctorAvailability pastSlot = new DoctorAvailability(doctor, today.minusDays(2).withHour(9).withMinute(0), today.minusDays(2).withHour(12).withMinute(0), true);
        DoctorAvailability futureSlot = new DoctorAvailability(doctor, today.plusDays(4).withHour(9).withMinute(0), today.plusDays(4).withHour(12).withMinute(0), true);

        availabilityRepository.save(pastSlot);
        availabilityRepository.save(futureSlot);

        List<DoctorAvailability> futureSlots = availabilityRepository.findByDoctorIdAndStartTimeAfter(doctor.getId(), today);
        assertEquals(1, futureSlots.size());
        assertEquals(futureSlot.getId(), futureSlots.get(0).getId());
    }

    @Test
    @DisplayName("5. Should fail validation when availability end time is before start time")
    void shouldFailValidationWhenEndTimeBeforeStartTime() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime invalidEnd = start.minusHours(2); // Ends before starting

        DoctorAvailability invalidSlot = new DoctorAvailability(doctor, start, invalidEnd, true);

        assertThrows(ConstraintViolationException.class, () -> {
            availabilityRepository.saveAndFlush(invalidSlot);
        });
    }

    @Test
    @DisplayName("6. Should fail validation when availability end time equals start time")
    void shouldFailValidationWhenEndTimeEqualsStartTime() {
        LocalDateTime sameTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        DoctorAvailability zeroDurationSlot = new DoctorAvailability(doctor, sameTime, sameTime, true);

        assertThrows(ConstraintViolationException.class, () -> {
            availabilityRepository.saveAndFlush(zeroDurationSlot);
        });
    }

    @Test
    @DisplayName("7. Should fail validation when availability owner does not have DOCTOR role")
    void shouldFailValidationWhenOwnerIsNotDoctor() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        // Attempt to create availability for a patient user
        DoctorAvailability invalidSlot = new DoctorAvailability(patient, start, end, true);

        assertThrows(ConstraintViolationException.class, () -> {
            availabilityRepository.saveAndFlush(invalidSlot);
        });
    }
}
