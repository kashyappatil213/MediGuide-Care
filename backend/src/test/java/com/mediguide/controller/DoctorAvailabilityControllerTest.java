package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.CreateAvailabilityRequest;
import com.mediguide.dto.CreateBatchAvailabilityRequest;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused integration tests for DoctorAvailabilityController.
 * Validates endpoint authorization, availability creation, chronological listing,
 * conflict-checked deactivation, and proper HTTP error response codes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorAvailabilityControllerTest {

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

    private User doctor1;
    private User doctor2;
    private User patient;

    private String doctor1Token;
    private String doctor2Token;
    private String patientToken;

    @BeforeEach
    void setUp() {
        doctor1 = userRepository.save(new User(
                "Dr. Alok Verma",
                "dr.alok." + System.nanoTime() + "@mediguide.local",
                "DoctorPass123",
                Role.DOCTOR
        ));
        doctor1Token = jwtService.generateToken(doctor1);

        doctor2 = userRepository.save(new User(
                "Dr. Priya Mehta",
                "dr.priya." + System.nanoTime() + "@mediguide.local",
                "DoctorPass123",
                Role.DOCTOR
        ));
        doctor2Token = jwtService.generateToken(doctor2);

        patient = userRepository.save(new User(
                "Rahul Sharma",
                "rahul." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patientToken = jwtService.generateToken(patient);
    }

    @Test
    @DisplayName("1. Doctor successfully creates availability: 201 Created with safe AvailabilityResponse")
    void shouldAllowDoctorToCreateAvailabilitySuccessfully() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(4);

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end, true);

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$.doctorName", is(doctor1.getFullName())))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("2. Doctor lists only their own availability slots, sorted chronologically: 200 OK")
    void shouldAllowDoctorToListOnlyOwnAvailabilitiesChronologically() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(8).withMinute(0).withSecond(0).withNano(0);

        // Save two slots for Doctor 1 (out of chronological order to test sorting)
        DoctorAvailability slot2 = availabilityRepository.save(
                new DoctorAvailability(doctor1, baseTime.plusHours(4), baseTime.plusHours(6), true)
        );
        DoctorAvailability slot1 = availabilityRepository.save(
                new DoctorAvailability(doctor1, baseTime, baseTime.plusHours(2), true)
        );

        // Save a slot for Doctor 2 (must not be visible to Doctor 1)
        availabilityRepository.save(
                new DoctorAvailability(doctor2, baseTime, baseTime.plusHours(4), true)
        );

        mockMvc.perform(get("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(slot1.getId().intValue())))
                .andExpect(jsonPath("$[0].doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$[1].id", is(slot2.getId().intValue())))
                .andExpect(jsonPath("$[1].doctorId", is(doctor1.getId().intValue())));
    }

    @Test
    @DisplayName("3. Doctor successfully deactivates their own availability slot: 200 OK")
    void shouldAllowDoctorToDeactivateOwnAvailabilitySlot() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(3);

        DoctorAvailability slot = availabilityRepository.save(
                new DoctorAvailability(doctor1, start, end, true)
        );

        mockMvc.perform(patch("/api/v1/doctor/availabilities/{id}/deactivate", slot.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(slot.getId().intValue())))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    @DisplayName("4a. Patient cannot create availability: 403 Forbidden")
    void shouldRejectPatientCreatingAvailability() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end, true);

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("4b. Patient cannot deactivate availability: 403 Forbidden")
    void shouldRejectPatientDeactivatingAvailability() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        DoctorAvailability slot = availabilityRepository.save(
                new DoctorAvailability(doctor1, start, start.plusHours(2), true)
        );

        mockMvc.perform(patch("/api/v1/doctor/availabilities/{id}/deactivate", slot.getId())
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("5. Missing availability slot returns 404 Not Found")
    void shouldReturn404WhenDeactivatingNonExistentSlot() throws Exception {
        mockMvc.perform(patch("/api/v1/doctor/availabilities/{id}/deactivate", 999999L)
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("6a. Invalid time interval (end before start) returns 400 Bad Request")
    void shouldReturn400WhenEndTimeBeforeStartTime() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.minusHours(2); // Inverted interval

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end, true);

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("6b. Slot creation in the past returns 400 Bad Request")
    void shouldReturn400WhenCreatingSlotInPast() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        CreateAvailabilityRequest request = new CreateAvailabilityRequest(start, end, true);

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("6c. Null required time fields return 400 Bad Request with field errors")
    void shouldReturn400WhenTimesAreNull() throws Exception {
        String jsonPayload = """
                {
                    "startTime": null,
                    "endTime": null,
                    "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.startTime", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.endTime", notNullValue()));
    }

    @Test
    @DisplayName("7. Deactivation with dependent future booked appointments returns 409 Conflict")
    void shouldReturn409WhenDeactivatingSlotWithFutureBookings() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(4);

        DoctorAvailability slot = availabilityRepository.save(
                new DoctorAvailability(doctor1, start, end, true)
        );

        // Book an appointment dependent on this slot
        appointmentRepository.save(new Appointment(
                patient, doctor1, slot, start, start.plusMinutes(30), AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/doctor/availabilities/{id}/deactivate", slot.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("8. Doctor cannot deactivate another doctor's availability slot: 403 Forbidden")
    void shouldRejectDoctorDeactivatingAnotherDoctorsSlot() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        DoctorAvailability slot = availabilityRepository.save(
                new DoctorAvailability(doctor2, start, start.plusHours(2), true)
        );

        // Doctor 1 attempts to deactivate Doctor 2's slot
        mockMvc.perform(patch("/api/v1/doctor/availabilities/{id}/deactivate", slot.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("9. Unauthenticated request to doctor availability endpoints returns 401 Unauthorized")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/availabilities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(post("/api/v1/doctor/availabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(patch("/api/v1/doctor/availabilities/1/deactivate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("10a. Patient can retrieve active future slots: 200 OK")
    void shouldAllowPatientToRetrieveActiveFutureAvailabilities() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);

        DoctorAvailability slot1 = availabilityRepository.save(
                new DoctorAvailability(doctor1, baseTime, baseTime.plusHours(2), true)
        );
        DoctorAvailability slot2 = availabilityRepository.save(
                new DoctorAvailability(doctor2, baseTime.plusHours(3), baseTime.plusHours(5), true)
        );

        mockMvc.perform(get("/api/v1/doctor/availabilities/active")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(slot1.getId().intValue())))
                .andExpect(jsonPath("$[0].doctorName", is(doctor1.getFullName())))
                .andExpect(jsonPath("$[1].id", is(slot2.getId().intValue())))
                .andExpect(jsonPath("$[1].doctorName", is(doctor2.getFullName())));
    }

    @Test
    @DisplayName("10b. Inactive slots are excluded from active discovery: 200 OK")
    void shouldExcludeInactiveSlotsFromActiveDiscovery() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);

        // Active slot
        DoctorAvailability activeSlot = availabilityRepository.save(
                new DoctorAvailability(doctor1, baseTime, baseTime.plusHours(2), true)
        );
        // Inactive slot
        availabilityRepository.save(
                new DoctorAvailability(doctor2, baseTime.plusHours(3), baseTime.plusHours(5), false)
        );

        mockMvc.perform(get("/api/v1/doctor/availabilities/active")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(activeSlot.getId().intValue())));
    }

    @Test
    @DisplayName("10c. Past slots are excluded from active discovery: 200 OK")
    void shouldExcludePastSlotsFromActiveDiscovery() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime future = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);

        // Past active slot
        availabilityRepository.save(
                new DoctorAvailability(doctor1, past, past.plusHours(2), true)
        );
        // Future active slot
        DoctorAvailability futureSlot = availabilityRepository.save(
                new DoctorAvailability(doctor2, future, future.plusHours(2), true)
        );

        mockMvc.perform(get("/api/v1/doctor/availabilities/active")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(futureSlot.getId().intValue())));
    }

    @Test
    @DisplayName("10d. Empty results return 200 OK with empty array")
    void shouldReturnEmptyListWhenNoActiveSlotsExist() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/availabilities/active")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("10e. Unauthenticated request to /active returns 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedForActive() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/availabilities/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("10f. Doctor is forbidden from active availability discovery: 403 Forbidden")
    void shouldRejectDoctorFromActiveDiscovery() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/availabilities/active")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("10g. Patient is forbidden from existing doctor availability list: 403 Forbidden")
    void shouldVerifyExistingDoctorAvailabilityListRemainsDoctorOnly() throws Exception {
        mockMvc.perform(get("/api/v1/doctor/availabilities")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("11a. Doctor creates multiple availability slots via /batch endpoint: 200 OK")
    void shouldAllowDoctorToCreateMultipleSlotsInBatch() throws Exception {
        LocalDateTime day = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(day, day.plusHours(2), true),
                new CreateAvailabilityRequest(day.plusHours(3), day.plusHours(5), true)
        );
        CreateBatchAvailabilityRequest request = new CreateBatchAvailabilityRequest(slots);

        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested", is(2)))
                .andExpect(jsonPath("$.totalCreated", is(2)))
                .andExpect(jsonPath("$.totalFailed", is(0)))
                .andExpect(jsonPath("$.successfulSlots", hasSize(2)))
                .andExpect(jsonPath("$.successfulSlots[0].doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$.rejectedSlots", hasSize(0)));
    }

    @Test
    @DisplayName("11b. Doctor creates batch with partial failure (conflict / overlap): 200 OK with breakdown")
    void shouldReportPartialFailureInBatchResponse() throws Exception {
        LocalDateTime day = LocalDateTime.now().plusDays(4).withHour(8).withMinute(0).withSecond(0).withNano(0);
        // Pre-existing active slot in database: 08:00 - 10:00
        availabilityRepository.save(new DoctorAvailability(doctor1, day, day.plusHours(2), true));

        // Batch: slot 1 overlaps existing (09:00 - 11:00); slot 2 is valid (13:00 - 16:00)
        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(day.plusHours(1), day.plusHours(3), true),
                new CreateAvailabilityRequest(day.plusHours(5), day.plusHours(8), true)
        );
        CreateBatchAvailabilityRequest request = new CreateBatchAvailabilityRequest(slots);

        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested", is(2)))
                .andExpect(jsonPath("$.totalCreated", is(1)))
                .andExpect(jsonPath("$.totalFailed", is(1)))
                .andExpect(jsonPath("$.successfulSlots", hasSize(1)))
                .andExpect(jsonPath("$.rejectedSlots", hasSize(1)))
                .andExpect(jsonPath("$.rejectedSlots[0].index", is(0)))
                .andExpect(jsonPath("$.rejectedSlots[0].reason", notNullValue()));
    }

    @Test
    @DisplayName("11c. Patient is forbidden from calling batch availability endpoint: 403 Forbidden")
    void shouldRejectPatientCallingBatchAvailability() throws Exception {
        LocalDateTime day = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        List<CreateAvailabilityRequest> slots = List.of(
                new CreateAvailabilityRequest(day, day.plusHours(2), true)
        );
        CreateBatchAvailabilityRequest request = new CreateBatchAvailabilityRequest(slots);

        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("11d. Unauthenticated request to /batch returns 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedForBatch() throws Exception {
        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slots\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("11e. Empty slot list or null request returns 400 Bad Request")
    void shouldReturn400WhenBatchRequestIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/doctor/availabilities/batch")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slots\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
