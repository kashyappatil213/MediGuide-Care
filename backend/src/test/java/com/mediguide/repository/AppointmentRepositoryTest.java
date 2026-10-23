package com.mediguide.repository;

import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private UserRepository userRepository;

    private User doctor;
    private User patient;
    private User anotherDoctor;
    private User anotherPatient;
    private DoctorAvailability availability;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;

    @BeforeEach
    void setUp() {
        doctor = new User("Dr. Ramesh Sen", "doctor.appt." + System.nanoTime() + "@mediguide.local", "hash123", Role.DOCTOR);
        userRepository.save(doctor);

        anotherDoctor = new User("Dr. Aisha Sharma", "doctor.other." + System.nanoTime() + "@mediguide.local", "hash123", Role.DOCTOR);
        userRepository.save(anotherDoctor);

        patient = new User("Anjali Gupta", "patient.appt." + System.nanoTime() + "@mediguide.local", "hash123", Role.PATIENT);
        userRepository.save(patient);

        anotherPatient = new User("Vikram Verma", "patient.other." + System.nanoTime() + "@mediguide.local", "hash123", Role.PATIENT);
        userRepository.save(anotherPatient);

        slotStart = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0);
        slotEnd = slotStart.plusHours(4); // 09:00 - 13:00

        availability = new DoctorAvailability(doctor, slotStart, slotEnd, true);
        availabilityRepository.save(availability);
    }

    @Test
    @DisplayName("1. Should persist Appointment with full relationships and verify default status and createdAt")
    void shouldPersistAppointmentWithRelationships() {
        LocalDateTime apptStart = slotStart.plusMinutes(30);
        LocalDateTime apptEnd = apptStart.plusMinutes(30);

        Appointment appointment = new Appointment(patient, doctor, availability, apptStart, apptEnd);
        Appointment saved = appointmentRepository.save(appointment);

        assertNotNull(saved.getId());
        assertEquals(patient.getId(), saved.getPatient().getId());
        assertEquals(doctor.getId(), saved.getDoctor().getId());
        assertEquals(availability.getId(), saved.getAvailability().getId());
        assertEquals(AppointmentStatus.BOOKED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());

        Optional<Appointment> found = appointmentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("2. Should persist and query appointments by status transitions (BOOKED, CANCELLED, COMPLETED)")
    void shouldPersistAndQueryStatusTransitions() {
        LocalDateTime start1 = slotStart.plusMinutes(15);
        LocalDateTime end1 = start1.plusMinutes(30);
        Appointment appt1 = new Appointment(patient, doctor, availability, start1, end1, AppointmentStatus.BOOKED);
        appointmentRepository.save(appt1);

        LocalDateTime start2 = slotStart.plusHours(1);
        LocalDateTime end2 = start2.plusMinutes(30);
        Appointment appt2 = new Appointment(anotherPatient, doctor, availability, start2, end2, AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt2);

        LocalDateTime start3 = slotStart.plusHours(2);
        LocalDateTime end3 = start3.plusMinutes(30);
        Appointment appt3 = new Appointment(patient, doctor, availability, start3, end3, AppointmentStatus.COMPLETED);
        appointmentRepository.save(appt3);

        List<Appointment> bookedList = appointmentRepository.findByDoctorIdAndStatus(doctor.getId(), AppointmentStatus.BOOKED);
        assertEquals(1, bookedList.size());
        assertEquals(appt1.getId(), bookedList.get(0).getId());

        List<Appointment> cancelledList = appointmentRepository.findByDoctorIdAndStatus(doctor.getId(), AppointmentStatus.CANCELLED);
        assertEquals(1, cancelledList.size());
        assertEquals(appt2.getId(), cancelledList.get(0).getId());

        List<Appointment> completedList = appointmentRepository.findByPatientIdAndStatus(patient.getId(), AppointmentStatus.COMPLETED);
        assertEquals(1, completedList.size());
        assertEquals(appt3.getId(), completedList.get(0).getId());
    }

    @Test
    @DisplayName("3. Should query appointments by patient, doctor, and availability")
    void shouldQueryByPatientDoctorAndAvailability() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);
        Appointment appt = new Appointment(patient, doctor, availability, start, end, AppointmentStatus.BOOKED);
        appointmentRepository.save(appt);

        List<Appointment> byPatient = appointmentRepository.findByPatientId(patient.getId());
        assertEquals(1, byPatient.size());

        List<Appointment> byDoctor = appointmentRepository.findByDoctorId(doctor.getId());
        assertEquals(1, byDoctor.size());

        List<Appointment> byAvail = appointmentRepository.findByAvailabilityId(availability.getId());
        assertEquals(1, byAvail.size());

        List<Appointment> byAvailStatus = appointmentRepository.findByAvailabilityIdAndStatus(availability.getId(), AppointmentStatus.BOOKED);
        assertEquals(1, byAvailStatus.size());
    }

    @Test
    @DisplayName("4. Should fail validation when appointment end time is before start time")
    void shouldFailValidationWhenEndTimeBeforeStartTime() {
        LocalDateTime start = slotStart.plusHours(1);
        LocalDateTime invalidEnd = start.minusMinutes(15);

        Appointment invalidAppt = new Appointment(patient, doctor, availability, start, invalidEnd);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("5. Should fail validation when appointment starts before availability interval")
    void shouldFailValidationWhenStartsBeforeAvailabilityInterval() {
        LocalDateTime earlyStart = slotStart.minusMinutes(30); // Starts before availability
        LocalDateTime end = slotStart.plusMinutes(30);

        Appointment invalidAppt = new Appointment(patient, doctor, availability, earlyStart, end);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("6. Should fail validation when appointment ends after availability interval")
    void shouldFailValidationWhenEndsAfterAvailabilityInterval() {
        LocalDateTime start = slotEnd.minusMinutes(15);
        LocalDateTime lateEnd = slotEnd.plusMinutes(30); // Overreaches past availability end

        Appointment invalidAppt = new Appointment(patient, doctor, availability, start, lateEnd);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("7. Should fail validation when patient and doctor are the same user")
    void shouldFailValidationWhenPatientEqualsDoctor() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);

        // Same user as both patient and doctor
        Appointment invalidAppt = new Appointment(doctor, doctor, availability, start, end);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("8. Should fail validation when patient does not have PATIENT role")
    void shouldFailValidationWhenPatientIsNotPatientRole() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);

        // anotherDoctor passed in as patient
        Appointment invalidAppt = new Appointment(anotherDoctor, doctor, availability, start, end);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("9. Should fail validation when doctor does not have DOCTOR role")
    void shouldFailValidationWhenDoctorIsNotDoctorRole() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);

        // patient passed in as doctor
        Appointment invalidAppt = new Appointment(patient, anotherPatient, availability, start, end);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("10. Should fail validation when doctor does not match availability owner")
    void shouldFailValidationWhenDoctorMismatchesAvailability() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);

        // anotherDoctor booked against doctor's availability slot
        Appointment invalidAppt = new Appointment(patient, anotherDoctor, availability, start, end);

        assertThrows(ConstraintViolationException.class, () -> {
            appointmentRepository.saveAndFlush(invalidAppt);
        });
    }

    @Test
    @DisplayName("11. Should retrieve appointment with pessimistic write lock via findByIdForUpdate")
    void shouldFindByIdForUpdateWithPessimisticLock() {
        LocalDateTime start = slotStart.plusMinutes(15);
        LocalDateTime end = start.plusMinutes(30);
        Appointment saved = appointmentRepository.save(new Appointment(patient, doctor, availability, start, end));

        Optional<Appointment> locked = appointmentRepository.findByIdForUpdate(saved.getId());
        assertTrue(locked.isPresent());
        assertEquals(saved.getId(), locked.get().getId());
        assertEquals(AppointmentStatus.BOOKED, locked.get().getStatus());
    }
}
