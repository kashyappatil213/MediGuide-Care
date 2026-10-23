package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.CreateConsultationRequest;
import com.mediguide.dto.CreatePrescriptionRequest;
import com.mediguide.dto.UpdateFollowUpRequest;
import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.ConsultationRecordRepository;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.PrescriptionRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused integration tests for ConsultationController.
 * Validates consultation records and prescription management endpoints,
 * participant authorization, lifecycle constraints, and error codes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsultationControllerTest {

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

    @Autowired
    private ConsultationRecordRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    private User doctor1;
    private User doctor2;
    private User patient1;
    private User patient2;

    private String doctor1Token;
    private String doctor2Token;
    private String patient1Token;
    private String patient2Token;

    private Appointment validAppointment;
    private Appointment cancelledAppointment;

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
                "Sunita Roy",
                "sunita." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patient2Token = jwtService.generateToken(patient2);

        LocalDateTime now = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor1, now, now.plusHours(4), true));

        validAppointment = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                slot,
                now,
                now.plusMinutes(30),
                AppointmentStatus.BOOKED
        ));

        cancelledAppointment = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                slot,
                now.plusHours(1),
                now.plusHours(1).plusMinutes(30),
                AppointmentStatus.CANCELLED
        ));
    }

    // --- Consultation Tests ---

    @Test
    @DisplayName("1. Assigned doctor creates consultation record: 201 Created")
    void shouldAllowAssignedDoctorToCreateConsultation() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest(
                "Patient exhibits mild acute bronchitis with persistent dry cough.",
                "Prescribed bronchodilator, warm hydration, and rest for 5 days."
        );

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.appointmentId", is(validAppointment.getId().intValue())))
                .andExpect(jsonPath("$.doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$.doctorName", is(doctor1.getFullName())))
                .andExpect(jsonPath("$.patientId", is(patient1.getId().intValue())))
                .andExpect(jsonPath("$.patientName", is(patient1.getFullName())))
                .andExpect(jsonPath("$.notes", is(request.getNotes())))
                .andExpect(jsonPath("$.treatment", is(request.getTreatment())));
    }

    @Test
    @DisplayName("2. Associated patient can view their consultation record: 200 OK")
    void shouldAllowPatientToViewConsultationRecord() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Diagnosis: Seasonal allergies", "Antihistamines once daily");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Diagnosis: Seasonal allergies")))
                .andExpect(jsonPath("$.treatment", is("Antihistamines once daily")));
    }

    @Test
    @DisplayName("3. Unrelated patient cannot view consultation record: 403 Forbidden")
    void shouldDenyUnrelatedPatientFromViewingConsultation() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Diagnosis: Seasonal allergies", "Antihistamines");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unrelated doctor cannot access or create consultation record: 403 Forbidden")
    void shouldDenyUnrelatedDoctorFromManagingConsultation() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Notes", "Treatment");

        // Unrelated doctor 2 attempts creation
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Patient cannot create consultation record: 403 Forbidden")
    void shouldDenyPatientFromCreatingConsultation() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Self diagnosis", "Rest");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Missing appointment returns 404 Not Found")
    void shouldReturn404WhenAppointmentNotFound() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Notes", "Treatment");

        mockMvc.perform(post("/api/v1/appointments/99999/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7. Cancelled appointment cannot receive consultation record: 409 Conflict")
    void shouldRejectConsultationForCancelledAppointment() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Notes", "Treatment");

        mockMvc.perform(post("/api/v1/appointments/" + cancelledAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("8. Duplicate consultation creation is rejected: 409 Conflict")
    void shouldRejectDuplicateConsultationCreation() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("Initial notes", "Treatment");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("9. Doctor can update existing consultation record: 200 OK")
    void shouldAllowDoctorToUpdateConsultationRecord() throws Exception {
        CreateConsultationRequest initial = new CreateConsultationRequest("Initial notes", "Treatment");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        CreateConsultationRequest updated = new CreateConsultationRequest("Updated diagnosis: viral fever", "Paracetamol 650mg TDS");
        mockMvc.perform(put("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Updated diagnosis: viral fever")))
                .andExpect(jsonPath("$.treatment", is("Paracetamol 650mg TDS")));
    }

    @Test
    @DisplayName("10. Validation error on blank consultation notes: 400 Bad Request")
    void shouldRejectBlankConsultationNotes() throws Exception {
        CreateConsultationRequest request = new CreateConsultationRequest("", "Treatment");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Consultation record not found returns 404")
    void shouldReturn404WhenConsultationNotYetCreated() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isNotFound());
    }

    // --- Prescription Tests ---

    @Test
    @DisplayName("12. Assigned doctor creates prescription item: 201 Created")
    void shouldAllowAssignedDoctorToCreatePrescription() throws Exception {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                "Amoxicillin 500mg",
                "1 capsule",
                "Three times daily after food",
                "7 days"
        );

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.appointmentId", is(validAppointment.getId().intValue())))
                .andExpect(jsonPath("$.medicationName", is(request.getMedicationName())))
                .andExpect(jsonPath("$.dosage", is(request.getDosage())))
                .andExpect(jsonPath("$.instructions", is(request.getInstructions())))
                .andExpect(jsonPath("$.duration", is(request.getDuration())));
    }

    @Test
    @DisplayName("13. Associated patient retrieves prescriptions: 200 OK")
    void shouldAllowPatientToRetrievePrescriptions() throws Exception {
        CreatePrescriptionRequest item1 = new CreatePrescriptionRequest("Paracetamol 650mg", "1 tab", "Twice daily", "3 days");
        CreatePrescriptionRequest item2 = new CreatePrescriptionRequest("Cetirizine 10mg", "1 tab", "At night", "5 days");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].medicationName", is("Paracetamol 650mg")))
                .andExpect(jsonPath("$[1].medicationName", is("Cetirizine 10mg")));
    }

    @Test
    @DisplayName("14. Unrelated patient cannot access prescription: 403 Forbidden")
    void shouldDenyUnrelatedPatientFromAccessingPrescription() throws Exception {
        CreatePrescriptionRequest item = new CreatePrescriptionRequest("Paracetamol 650mg", "1 tab", "Twice daily", "3 days");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + patient2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Unrelated doctor cannot access or create prescription: 403 Forbidden")
    void shouldDenyUnrelatedDoctorFromPrescriptionOperations() throws Exception {
        CreatePrescriptionRequest item = new CreatePrescriptionRequest("Paracetamol 650mg", "1 tab", "Twice daily", "3 days");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("16. Patient cannot create prescription: 403 Forbidden")
    void shouldDenyPatientFromCreatingPrescription() throws Exception {
        CreatePrescriptionRequest item = new CreatePrescriptionRequest("Med", "Dosage", "Inst", "Dur");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("17. Cancelled appointment cannot receive prescription: 409 Conflict")
    void shouldRejectPrescriptionForCancelledAppointment() throws Exception {
        CreatePrescriptionRequest item = new CreatePrescriptionRequest("Med", "Dosage", "Inst", "Dur");

        mockMvc.perform(post("/api/v1/appointments/" + cancelledAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("18. Missing required fields in prescription request returns 400 Bad Request")
    void shouldRejectMissingFieldsInPrescription() throws Exception {
        CreatePrescriptionRequest item = new CreatePrescriptionRequest("", "", "", "");

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/prescription")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("19. Unauthenticated requests return 401 Unauthorized")
    void shouldReturn401ForUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/consultation"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/prescription"))
                .andExpect(status().isUnauthorized());
    }

    // --- Follow-up Scheduling Tests ---

    @Test
    @DisplayName("20. Assigned doctor creates consultation with follow-up date and notes: 201 Created")
    void shouldAllowAssignedDoctorToCreateConsultationWithFollowUp() throws Exception {
        LocalDate followUpDate = LocalDate.now().plusWeeks(2);
        String followUpNotes = "Review chest X-ray and check blood pressure.";

        CreateConsultationRequest request = new CreateConsultationRequest(
                "Bronchial congestion diagnosed.",
                "Inhaler prescribed twice daily.",
                followUpDate,
                followUpNotes
        );

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.followUpDate", is(followUpDate.toString())))
                .andExpect(jsonPath("$.followUpNotes", is(followUpNotes)));
    }

    @Test
    @DisplayName("21. Patient can view follow-up date and notes in their consultation record: 200 OK")
    void shouldAllowPatientToViewFollowUpInformation() throws Exception {
        LocalDate followUpDate = LocalDate.now().plusWeeks(3);
        String followUpNotes = "Check wound healing and suture removal.";

        CreateConsultationRequest request = new CreateConsultationRequest(
                "Post-op dressing clean.",
                "Keep area dry and clean.",
                followUpDate,
                followUpNotes
        );

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpDate", is(followUpDate.toString())))
                .andExpect(jsonPath("$.followUpNotes", is(followUpNotes)));
    }

    @Test
    @DisplayName("22. Doctor updates follow-up information via PUT consultation: 200 OK")
    void shouldAllowDoctorToUpdateFollowUpViaPut() throws Exception {
        CreateConsultationRequest initial = new CreateConsultationRequest("Initial notes", "Treatment");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followUpDate").doesNotExist());

        LocalDate newFollowUp = LocalDate.now().plusMonths(1);
        CreateConsultationRequest updated = new CreateConsultationRequest(
                "Initial notes",
                "Treatment",
                newFollowUp,
                "Routine checkup"
        );

        mockMvc.perform(put("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpDate", is(newFollowUp.toString())))
                .andExpect(jsonPath("$.followUpNotes", is("Routine checkup")));
    }

    @Test
    @DisplayName("23. Doctor updates follow-up information via PATCH endpoint: 200 OK")
    void shouldAllowDoctorToUpdateFollowUpViaPatch() throws Exception {
        CreateConsultationRequest initial = new CreateConsultationRequest("Initial notes", "Treatment");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        LocalDate followUpDate = LocalDate.now().plusDays(10);
        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(followUpDate, "Repeat lab work.");

        mockMvc.perform(patch("/api/v1/appointments/" + validAppointment.getId() + "/consultation/follow-up")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpDate", is(followUpDate.toString())))
                .andExpect(jsonPath("$.followUpNotes", is("Repeat lab work.")));
    }

    @Test
    @DisplayName("24. Past follow-up date is rejected on consultation creation: 400 Bad Request")
    void shouldRejectPastFollowUpDateOnCreate() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        CreateConsultationRequest request = new CreateConsultationRequest(
                "Notes",
                "Treatment",
                pastDate,
                "Follow-up in the past"
        );

        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("25. Past follow-up date is rejected on PATCH: 400 Bad Request")
    void shouldRejectPastFollowUpDateOnPatch() throws Exception {
        CreateConsultationRequest initial = new CreateConsultationRequest("Notes", "Treatment");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(LocalDate.now().minusWeeks(1), "Past date");

        mockMvc.perform(patch("/api/v1/appointments/" + validAppointment.getId() + "/consultation/follow-up")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("26. Follow-up notes exceeding 1000 characters is rejected: 400 Bad Request")
    void shouldRejectExcessiveFollowUpNotes() throws Exception {
        CreateConsultationRequest initial = new CreateConsultationRequest("Notes", "Treatment");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        String longNotes = "a".repeat(1001);
        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(LocalDate.now().plusDays(5), longNotes);

        mockMvc.perform(patch("/api/v1/appointments/" + validAppointment.getId() + "/consultation/follow-up")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("27. Unrelated doctor cannot update follow-up: 403 Forbidden")
    void shouldDenyUnrelatedDoctorFromUpdatingFollowUp() throws Exception {
        CreateConsultationRequest initial = new CreateConsultationRequest("Notes", "Treatment");
        mockMvc.perform(post("/api/v1/appointments/" + validAppointment.getId() + "/consultation")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(LocalDate.now().plusDays(5), "Notes");

        mockMvc.perform(patch("/api/v1/appointments/" + validAppointment.getId() + "/consultation/follow-up")
                        .header("Authorization", "Bearer " + doctor2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("28. Patient cannot update follow-up: 403 Forbidden")
    void shouldDenyPatientFromUpdatingFollowUp() throws Exception {
        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(LocalDate.now().plusDays(5), "Patient notes");

        mockMvc.perform(patch("/api/v1/appointments/" + validAppointment.getId() + "/consultation/follow-up")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("29. Follow-up update rejected for cancelled appointment: 409 Conflict")
    void shouldRejectFollowUpForCancelledAppointment() throws Exception {
        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(LocalDate.now().plusDays(5), "Cancelled appt notes");

        mockMvc.perform(patch("/api/v1/appointments/" + cancelledAppointment.getId() + "/consultation/follow-up")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("30. Follow-up update on non-existent appointment returns 404 Not Found")
    void shouldReturn404WhenAppointmentNotFoundForFollowUp() throws Exception {
        UpdateFollowUpRequest patchRequest = new UpdateFollowUpRequest(LocalDate.now().plusDays(5), "Notes");

        mockMvc.perform(patch("/api/v1/appointments/999999/consultation/follow-up")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isNotFound());
    }
}

