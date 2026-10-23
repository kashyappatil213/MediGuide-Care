package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.BookAppointmentRequest;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused integration tests for AppointmentController.
 * Validates endpoint authorization, booking workflows, personal listing,
 * conflict and concurrency rule enforcement, and OpenAPI schema documentation availability.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppointmentControllerTest {

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
    private User patient1;
    private User patient2;
    private User admin;

    private String doctor1Token;
    private String doctor2Token;
    private String patient1Token;
    private String patient2Token;
    private String adminToken;

    private DoctorAvailability doctor1Slot;
    private DoctorAvailability doctor2Slot;

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

        patient1 = userRepository.save(new User(
                "Rahul Sharma",
                "rahul." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patient1Token = jwtService.generateToken(patient1);

        patient2 = userRepository.save(new User(
                "Sunita Rao",
                "sunita." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patient2Token = jwtService.generateToken(patient2);

        admin = userRepository.save(new User(
                "System Admin",
                "admin." + System.nanoTime() + "@mediguide.local",
                "AdminPass123",
                Role.ADMIN
        ));
        adminToken = jwtService.generateToken(admin);

        LocalDateTime baseStart = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime baseEnd = baseStart.plusHours(4);

        doctor1Slot = availabilityRepository.save(
                new DoctorAvailability(doctor1, baseStart, baseEnd, true)
        );

        doctor2Slot = availabilityRepository.save(
                new DoctorAvailability(doctor2, baseStart, baseEnd, true)
        );
    }

    @Test
    @DisplayName("1. Patient successfully books a valid appointment: 201 Created with safe AppointmentResponse")
    void shouldAllowPatientToBookValidAppointmentSuccessfully() throws Exception {
        LocalDateTime start = doctor1Slot.getStartTime().plusMinutes(30);
        LocalDateTime end = start.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(
                doctor1Slot.getId(),
                start,
                end
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.patientId", is(patient1.getId().intValue())))
                .andExpect(jsonPath("$.patientName", is(patient1.getFullName())))
                .andExpect(jsonPath("$.doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$.doctorName", is(doctor1.getFullName())))
                .andExpect(jsonPath("$.availabilityId", is(doctor1Slot.getId().intValue())))
                .andExpect(jsonPath("$.status", is("BOOKED")))
                .andExpect(jsonPath("$.startTime", notNullValue()))
                .andExpect(jsonPath("$.endTime", notNullValue()));
    }

    @Test
    @DisplayName("2a. Invalid request body with null required fields returns 400 Bad Request")
    void shouldReturn400WhenBookingWithNullRequiredFields() throws Exception {
        String jsonPayload = """
                {
                    "availabilityId": null,
                    "startTime": null,
                    "endTime": null
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.availabilityId", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.startTime", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.endTime", notNullValue()));
    }

    @Test
    @DisplayName("2b. End time before start time returns 400 Bad Request")
    void shouldReturn400WhenEndTimeBeforeStartTime() throws Exception {
        LocalDateTime start = doctor1Slot.getStartTime().plusHours(1);
        LocalDateTime end = start.minusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(doctor1Slot.getId(), start, end);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("end time must be strictly after start time")));
    }

    @Test
    @DisplayName("2c. Booking in the past returns 400 Bad Request")
    void shouldReturn400WhenBookingInPast() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(doctor1Slot.getId(), start, end);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("in the past")));
    }

    @Test
    @DisplayName("2d. Booking outside availability slot boundaries returns 400 Bad Request")
    void shouldReturn400WhenBookingOutsideAvailabilitySlot() throws Exception {
        // Slot is 9:00 - 13:00, booking at 13:00 - 14:00
        LocalDateTime start = doctor1Slot.getEndTime();
        LocalDateTime end = start.plusHours(1);

        BookAppointmentRequest request = new BookAppointmentRequest(doctor1Slot.getId(), start, end);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("falls outside")));
    }

    @Test
    @DisplayName("2e. Booking against inactive availability slot returns 400 Bad Request")
    void shouldReturn400WhenBookingAgainstInactiveSlot() throws Exception {
        DoctorAvailability inactiveSlot = availabilityRepository.save(new DoctorAvailability(
                doctor1,
                doctor1Slot.getStartTime().plusDays(5),
                doctor1Slot.getEndTime().plusDays(5),
                false
        ));

        BookAppointmentRequest request = new BookAppointmentRequest(
                inactiveSlot.getId(),
                inactiveSlot.getStartTime(),
                inactiveSlot.getStartTime().plusMinutes(30)
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("not active")));
    }

    @Test
    @DisplayName("2f. Malformed JSON request body returns 400 Bad Request")
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ malformed_json: true,"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("3. Unauthenticated access returns 401 Unauthorized across all endpoints")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/appointments/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/v1/appointments/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("4. Doctor cannot use the patient-only booking endpoint: 403 Forbidden")
    void shouldRejectDoctorFromUsingPatientOnlyBookingEndpoint() throws Exception {
        BookAppointmentRequest request = new BookAppointmentRequest(
                doctor1Slot.getId(),
                doctor1Slot.getStartTime(),
                doctor1Slot.getStartTime().plusMinutes(30)
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("5. Patient cannot retrieve another patient's appointment: 403 Forbidden")
    void shouldRejectPatientRetrievingAnotherPatientsAppointment() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient2,
                doctor1,
                doctor1Slot,
                doctor1Slot.getStartTime(),
                doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(get("/api/v1/appointments/{id}", appt.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("6. Unrelated doctor cannot retrieve an appointment by ID: 403 Forbidden")
    void shouldRejectUnrelatedDoctorRetrievingAppointmentById() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                doctor1Slot,
                doctor1Slot.getStartTime(),
                doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(get("/api/v1/appointments/{id}", appt.getId())
                        .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("7a. Authorized patient can retrieve their own appointment by ID: 200 OK")
    void shouldAllowPatientToRetrieveOwnAppointment() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                doctor1Slot,
                doctor1Slot.getStartTime(),
                doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(get("/api/v1/appointments/{id}", appt.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appt.getId().intValue())))
                .andExpect(jsonPath("$.patientId", is(patient1.getId().intValue())))
                .andExpect(jsonPath("$.doctorId", is(doctor1.getId().intValue())));
    }

    @Test
    @DisplayName("7b. Assigned doctor can retrieve their consultation appointment by ID: 200 OK")
    void shouldAllowAssignedDoctorToRetrieveAppointment() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                doctor1Slot,
                doctor1Slot.getStartTime(),
                doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(get("/api/v1/appointments/{id}", appt.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appt.getId().intValue())))
                .andExpect(jsonPath("$.doctorId", is(doctor1.getId().intValue())));
    }

    @Test
    @DisplayName("8. Patient listing returns only the authenticated patient's appointments: 200 OK")
    void shouldReturnOnlyAuthenticatedPatientsAppointmentsOnMyEndpoint() throws Exception {
        LocalDateTime base = doctor1Slot.getStartTime();
        appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot, base, base.plusMinutes(30), AppointmentStatus.BOOKED
        ));
        appointmentRepository.save(new Appointment(
                patient1, doctor2, doctor2Slot, base.plusHours(1), base.plusHours(1).plusMinutes(30), AppointmentStatus.BOOKED
        ));
        appointmentRepository.save(new Appointment(
                patient2, doctor1, doctor1Slot, base.plusHours(2), base.plusHours(2).plusMinutes(30), AppointmentStatus.BOOKED
        ));

        mockMvc.perform(get("/api/v1/appointments/my")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].patientId", is(patient1.getId().intValue())))
                .andExpect(jsonPath("$[1].patientId", is(patient1.getId().intValue())));
    }

    @Test
    @DisplayName("9. Doctor listing returns only the authenticated doctor's appointments: 200 OK")
    void shouldReturnOnlyAuthenticatedDoctorsAppointmentsOnMyEndpoint() throws Exception {
        LocalDateTime base = doctor1Slot.getStartTime();
        appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot, base, base.plusMinutes(30), AppointmentStatus.BOOKED
        ));
        appointmentRepository.save(new Appointment(
                patient2, doctor1, doctor1Slot, base.plusMinutes(30), base.plusHours(1), AppointmentStatus.BOOKED
        ));
        appointmentRepository.save(new Appointment(
                patient1, doctor2, doctor2Slot, base.plusHours(2), base.plusHours(2).plusMinutes(30), AppointmentStatus.BOOKED
        ));

        mockMvc.perform(get("/api/v1/appointments/my")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$[1].doctorId", is(doctor1.getId().intValue())));
    }

    @Test
    @DisplayName("10a. Overlapping booking for same doctor returns 409 Conflict")
    void shouldReturn409WhenBookingOverlapsDoctorExistingAppointment() throws Exception {
        LocalDateTime start = doctor1Slot.getStartTime();
        LocalDateTime end = start.plusMinutes(30);

        appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot, start, end, AppointmentStatus.BOOKED
        ));

        BookAppointmentRequest request = new BookAppointmentRequest(
                doctor1Slot.getId(),
                start.plusMinutes(15),
                end.plusMinutes(15)
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already has an overlapping appointment")));
    }

    @Test
    @DisplayName("10b. Overlapping booking for same patient across doctors returns 409 Conflict")
    void shouldReturn409WhenBookingOverlapsPatientExistingAppointment() throws Exception {
        LocalDateTime start = doctor1Slot.getStartTime();
        LocalDateTime end = start.plusMinutes(30);

        appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot, start, end, AppointmentStatus.BOOKED
        ));

        BookAppointmentRequest request = new BookAppointmentRequest(
                doctor2Slot.getId(),
                start.plusMinutes(10),
                end.plusMinutes(10)
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Patient already has an overlapping appointment")));
    }

    @Test
    @DisplayName("11. Missing appointment returns 404 Not Found")
    void shouldReturn404WhenAppointmentNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/{id}", 999999L)
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Appointment not found")));
    }

    @Test
    @DisplayName("12. Patient cannot book on behalf of another patient ID: 403 Forbidden")
    void shouldRejectPatientBookingForAnotherPatientId() throws Exception {
        LocalDateTime start = doctor1Slot.getStartTime();
        LocalDateTime end = start.plusMinutes(30);

        BookAppointmentRequest request = new BookAppointmentRequest(
                doctor1Slot.getId(),
                start,
                end,
                patient2.getId()
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("13. Unsupported role rejected for personal appointment listing: 403 Forbidden")
    void shouldRejectUnsupportedRoleForPersonalAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/my")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("14. OpenAPI schema documentation endpoint loads and documents appointment/availability endpoints")
    void shouldLoadOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/appointments']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/appointments/{id}']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/appointments/my']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/appointments/{id}/cancel']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/appointments/{id}/complete']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/doctor/availabilities']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/doctor/availabilities/active']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/doctor/availabilities/{id}/deactivate']", notNullValue()));
    }

    // ==================== CANCELLATION ENDPOINT TESTS ====================

    @Test
    @DisplayName("15a. Assigned patient can cancel booked appointment: 200 OK with safe AppointmentResponse")
    void shouldAllowAssignedPatientToCancelAppointment() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appt.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appt.getId().intValue())))
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        Appointment persisted = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, persisted.getStatus());
    }

    @Test
    @DisplayName("15b. Assigned doctor can cancel booked appointment: 200 OK with safe AppointmentResponse")
    void shouldAllowAssignedDoctorToCancelAppointment() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appt.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appt.getId().intValue())))
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        Appointment persisted = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, persisted.getStatus());
    }

    @Test
    @DisplayName("15c. Unrelated patient cannot cancel appointment: 403 Forbidden")
    void shouldRejectUnrelatedPatientFromCancelling() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appt.getId())
                        .header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("15d. Unrelated doctor cannot cancel appointment: 403 Forbidden")
    void shouldRejectUnrelatedDoctorFromCancelling() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appt.getId())
                        .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("15e. Admin cannot cancel appointment: 403 Forbidden")
    void shouldRejectAdminFromCancelling() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appt.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("15f. Unauthenticated cancel request returns 401 Unauthorized")
    void shouldReturn401WhenCancellingUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/1/cancel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("15g. Cancelling non-existent appointment returns 404 Not Found")
    void shouldReturn404WhenCancellingNonExistent() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", 999999L)
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("15h. Cancelling already cancelled or completed appointment returns 409 Conflict")
    void shouldReturn409WhenCancellingNonBookedAppointment() throws Exception {
        Appointment cancelledAppt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.CANCELLED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", cancelledAppt.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));

        Appointment completedAppt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime().plusHours(1), doctor1Slot.getStartTime().plusHours(1).plusMinutes(30),
                AppointmentStatus.COMPLETED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", completedAppt.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    // ==================== COMPLETION ENDPOINT TESTS ====================

    @Test
    @DisplayName("16a. Assigned doctor can complete booked appointment: 200 OK with safe AppointmentResponse")
    void shouldAllowAssignedDoctorToCompleteAppointment() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appt.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appt.getId().intValue())))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        Appointment persisted = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.COMPLETED, persisted.getStatus());
    }

    @Test
    @DisplayName("16b. Patient cannot complete appointment: 403 Forbidden")
    void shouldRejectPatientFromCompleting() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appt.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("16c. Unrelated doctor cannot complete appointment: 403 Forbidden")
    void shouldRejectUnrelatedDoctorFromCompleting() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appt.getId())
                        .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("16d. Admin cannot complete appointment: 403 Forbidden")
    void shouldRejectAdminFromCompleting() throws Exception {
        Appointment appt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appt.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("16e. Unauthenticated complete request returns 401 Unauthorized")
    void shouldReturn401WhenCompletingUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/1/complete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("16f. Completing non-existent appointment returns 404 Not Found")
    void shouldReturn404WhenCompletingNonExistent() throws Exception {
        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", 999999L)
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("16g. Completing cancelled or already completed appointment returns 409 Conflict")
    void shouldReturn409WhenCompletingNonBookedAppointment() throws Exception {
        Appointment cancelledAppt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime(), doctor1Slot.getStartTime().plusMinutes(30),
                AppointmentStatus.CANCELLED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", cancelledAppt.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));

        Appointment completedAppt = appointmentRepository.save(new Appointment(
                patient1, doctor1, doctor1Slot,
                doctor1Slot.getStartTime().plusHours(1), doctor1Slot.getStartTime().plusHours(1).plusMinutes(30),
                AppointmentStatus.COMPLETED
        ));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", completedAppt.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }
}
