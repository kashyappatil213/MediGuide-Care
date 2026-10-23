package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.BookAppointmentRequest;
import com.mediguide.dto.CreateAvailabilityRequest;
import com.mediguide.dto.CreateBatchAvailabilityRequest;
import com.mediguide.dto.CreateConsultationRequest;
import com.mediguide.dto.CreatePrescriptionRequest;
import com.mediguide.dto.UpdateFollowUpRequest;
import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.UserRepository;
import com.mediguide.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test validating the complete MediGuide Care consultation lifecycle:
 * 1. Doctor multiple time-slot pre-scheduling and overlap conflict protection.
 * 2. Patient slot viewing and appointment booking.
 * 3. Doctor appointment completion, clinical notes, prescriptions, and follow-up recommendation.
 * 4. Doctor consultation history retrieval and patient filtering.
 * 5. Patient consultation history retrieval (verifying follow-up is an advisory recommendation).
 * 6. Doctor and patient security boundaries, access controls, and regression protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompleteWorkflowEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorAvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private User doctor;
    private User patient;
    private User unrelatedDoctor;
    private User otherPatient;

    private String doctorToken;
    private String patientToken;
    private String unrelatedDoctorToken;
    private String otherPatientToken;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        doctor = userRepository.save(new User("Dr. Sarah Jenkins", "doctor." + ts + "@mediguide.local", "hashedPass123", Role.DOCTOR));
        patient = userRepository.save(new User("Anjali Gupta", "patient." + ts + "@mediguide.local", "hashedPass123", Role.PATIENT));
        unrelatedDoctor = userRepository.save(new User("Dr. Bruce Banner", "unrelated.doc." + ts + "@mediguide.local", "hashedPass123", Role.DOCTOR));
        otherPatient = userRepository.save(new User("Vikram Sharma", "other.patient." + ts + "@mediguide.local", "hashedPass123", Role.PATIENT));

        doctorToken = "Bearer " + jwtService.generateToken(doctor);
        patientToken = "Bearer " + jwtService.generateToken(patient);
        unrelatedDoctorToken = "Bearer " + jwtService.generateToken(unrelatedDoctor);
        otherPatientToken = "Bearer " + jwtService.generateToken(otherPatient);
    }

    @Test
    @DisplayName("Complete E2E Workflow: Pre-scheduling -> Booking -> Completion -> Clinical Notes & Rx -> Follow-up -> History Inspection")
    void completeEndToEndWorkflowVerification() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime slot1Start = baseTime;
        LocalDateTime slot1End = baseTime.plusMinutes(30);

        LocalDateTime slot2Start = baseTime.plusMinutes(45);
        LocalDateTime slot2End = baseTime.plusMinutes(75);

        // 1. Doctor creates multiple future availability slots via batch pre-scheduling
        CreateBatchAvailabilityRequest batchRequest = new CreateBatchAvailabilityRequest(List.of(
                new CreateAvailabilityRequest(slot1Start, slot1End),
                new CreateAvailabilityRequest(slot2Start, slot2End)
        ));

        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated", is(2)))
                .andExpect(jsonPath("$.successfulSlots", hasSize(2)));

        // 2. Overlap conflict protection: Attempting to create an individual slot overlapping slot 1 must return 409 Conflict
        CreateAvailabilityRequest overlappingRequest = new CreateAvailabilityRequest(
                slot1Start.plusMinutes(10), slot1End.plusMinutes(15)
        );

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("overlapping")));

        // Also test batch overlap rejection
        CreateBatchAvailabilityRequest batchOverlap = new CreateBatchAvailabilityRequest(List.of(
                new CreateAvailabilityRequest(slot1Start.plusMinutes(5), slot1End.plusMinutes(10))
        ));

        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchOverlap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated", is(0)))
                .andExpect(jsonPath("$.totalFailed", is(1)))
                .andExpect(jsonPath("$.rejectedSlots[0].reason", containsString("Overlaps")));

        // 3. Patient views active doctor availabilities
        MvcResult activeSlotsResult = mockMvc.perform(get("/api/v1/doctor/availabilities/active")
                        .header("Authorization", patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();

        Long availabilityId = objectMapper.readTree(activeSlotsResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        // 4. Patient books an appointment for Slot 1
        BookAppointmentRequest bookRequest = new BookAppointmentRequest(availabilityId, slot1Start, slot1End);

        MvcResult bookResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doctorId", is(doctor.getId().intValue())))
                .andExpect(jsonPath("$.patientId", is(patient.getId().intValue())))
                .andExpect(jsonPath("$.status", is("BOOKED")))
                .andReturn();

        String bookResponseStr = bookResult.getResponse().getContentAsString();
        Long appointmentId = objectMapper.readTree(bookResponseStr).get("id").asLong();

        // 5. Doctor views appointments and completes the appointment
        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId + "/complete")
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // 6. Doctor records clinical notes, treatment instructions, and recommended follow-up date
        LocalDate followUpDate = LocalDate.now().plusWeeks(2);
        CreateConsultationRequest consultRequest = new CreateConsultationRequest(
                "Patient presented with seasonal acute sinusitis and congestion.",
                "Advised warm saline irrigation twice daily and increased water intake.",
                followUpDate,
                "Review nasal symptoms and evaluate response to amoxicillin course."
        );

        mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/consultation")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consultRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId", is(appointmentId.intValue())))
                .andExpect(jsonPath("$.notes", containsString("acute sinusitis")))
                .andExpect(jsonPath("$.treatment", containsString("saline irrigation")))
                .andExpect(jsonPath("$.followUpDate", is(followUpDate.toString())));

        // 7. Doctor adds prescribed medications
        CreatePrescriptionRequest rx1 = new CreatePrescriptionRequest("Amoxicillin", "500mg", "Take 1 capsule 3 times daily with meals", "7 days");
        CreatePrescriptionRequest rx2 = new CreatePrescriptionRequest("Cetirizine", "10mg", "Take 1 tablet at bedtime", "10 days");

        mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/prescription")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rx1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationName", is("Amoxicillin")));

        mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/prescription")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rx2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationName", is("Cetirizine")));

        // 8. Doctor reviews clinical consultation history (all records and filtered by patient)
        mockMvc.perform(get("/api/v1/consultations/history")
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].appointmentId", is(appointmentId.intValue())))
                .andExpect(jsonPath("$[0].patientId", is(patient.getId().intValue())))
                .andExpect(jsonPath("$[0].followUpDate", is(followUpDate.toString())))
                .andExpect(jsonPath("$[0].prescriptions", hasSize(2)));

        mockMvc.perform(get("/api/v1/consultations/history/patient/" + patient.getId())
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientId", is(patient.getId().intValue())));

        // 9. Patient reviews their own consultation history
        mockMvc.perform(get("/api/v1/consultations/history")
                        .header("Authorization", patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].appointmentId", is(appointmentId.intValue())))
                .andExpect(jsonPath("$[0].doctorId", is(doctor.getId().intValue())))
                .andExpect(jsonPath("$[0].doctorName", is(doctor.getFullName())))
                .andExpect(jsonPath("$[0].notes", containsString("acute sinusitis")))
                .andExpect(jsonPath("$[0].treatment", containsString("saline irrigation")))
                .andExpect(jsonPath("$[0].followUpDate", is(followUpDate.toString())))
                .andExpect(jsonPath("$[0].prescriptions", hasSize(2)));

        // 10. Verify follow-up is an advisory recommendation only (no automatic new appointment booked)
        List<Appointment> allAppointments = appointmentRepository.findAll();
        long matchingAppointments = allAppointments.stream()
                .filter(a -> a.getPatient().getId().equals(patient.getId()))
                .count();
        // Exactly 1 appointment was booked; the follow-up recommendation did NOT create a new appointment record
        assertFalse(matchingAppointments > 1, "Follow-up must remain an advisory recommendation and not auto-book a new appointment");
    }

    @Test
    @DisplayName("Security & Access Boundaries: Privacy rules, cross-role protection, and regression checks")
    void securityAndRegressionBoundaries() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(
                doctor, baseTime, baseTime.plusMinutes(30)
        ));

        // Create an appointment between doctor and patient
        Appointment appointment = appointmentRepository.save(new Appointment(
                patient, doctor, slot, slot.getStartTime(), slot.getEndTime(), AppointmentStatus.COMPLETED
        ));

        // Consultation record with follow-up
        mockMvc.perform(post("/api/v1/appointments/" + appointment.getId() + "/consultation")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateConsultationRequest(
                                "Routine checkup", "Normal vitals"
                        ))))
                .andExpect(status().isCreated());

        // 1. Unauthenticated request without token must return 401
        mockMvc.perform(get("/api/v1/consultations/history"))
                .andExpect(status().isUnauthorized());

        // 2. Patient cannot access another patient's consultation history (403 Forbidden)
        mockMvc.perform(get("/api/v1/consultations/history/patient/" + otherPatient.getId())
                        .header("Authorization", patientToken))
                .andExpect(status().isForbidden());

        // 3. Unrelated doctor who has never consulted the patient cannot access patient's history (403 Forbidden)
        mockMvc.perform(get("/api/v1/consultations/history/patient/" + patient.getId())
                        .header("Authorization", unrelatedDoctorToken))
                .andExpect(status().isForbidden());

        // 4. Follow-up date in the past must be rejected with 400 Bad Request
        UpdateFollowUpRequest pastFollowUp = new UpdateFollowUpRequest(LocalDate.now().minusDays(1), "Invalid past date");
        mockMvc.perform(patch("/api/v1/appointments/" + appointment.getId() + "/consultation/follow-up")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastFollowUp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("past")));

        // 5. Already completed appointment cannot be cancelled (409 Conflict)
        mockMvc.perform(patch("/api/v1/appointments/" + appointment.getId() + "/cancel")
                        .header("Authorization", doctorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("COMPLETED")));

        // 6. Response does not leak password hashes or JWT secrets
        mockMvc.perform(get("/api/v1/consultations/history")
                        .header("Authorization", patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[0].secret").doesNotExist());
    }
}
