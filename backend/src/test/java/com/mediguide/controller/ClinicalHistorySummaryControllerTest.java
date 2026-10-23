package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.dto.ClinicalHistorySummaryRequest;
import com.mediguide.exception.AiConfigurationException;
import com.mediguide.exception.AiServiceException;
import com.mediguide.model.Appointment;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.ConsultationRecord;
import com.mediguide.model.DoctorAvailability;
import com.mediguide.model.Prescription;
import com.mediguide.model.Role;
import com.mediguide.model.User;
import com.mediguide.repository.AppointmentRepository;
import com.mediguide.repository.ConsultationRecordRepository;
import com.mediguide.repository.DoctorAvailabilityRepository;
import com.mediguide.repository.PrescriptionRepository;
import com.mediguide.repository.UserRepository;
import com.mediguide.service.ClinicalHistorySummaryService;
import com.mediguide.service.ClinicalSummaryAiClient;
import com.mediguide.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClinicalHistorySummaryControllerTest {

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

    @MockBean
    private ClinicalSummaryAiClient aiClient;

    private User doctor1;
    private User doctor2;
    private User patient1;
    private User patient2;
    private User patientNoHistory;

    private String doctor1Token;
    private String doctor2Token;
    private String patient1Token;

    private Appointment app1;
    private Appointment app2;
    private ConsultationRecord consult1;
    private ConsultationRecord consult2;

    @BeforeEach
    void setUp() {
        doctor1 = userRepository.save(new User(
                "Dr. Neha Sharma",
                "dr.neha." + System.nanoTime() + "@mediguide.local",
                "DoctorPass123",
                Role.DOCTOR
        ));
        doctor1Token = jwtService.generateToken(doctor1);

        doctor2 = userRepository.save(new User(
                "Dr. Rajesh Verma",
                "dr.rajesh." + System.nanoTime() + "@mediguide.local",
                "DoctorPass123",
                Role.DOCTOR
        ));
        doctor2Token = jwtService.generateToken(doctor2);

        patient1 = userRepository.save(new User(
                "Rohan Gupta",
                "rohan." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patient1Token = jwtService.generateToken(patient1);

        patient2 = userRepository.save(new User(
                "Sneha Roy",
                "sneha." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));

        patientNoHistory = userRepository.save(new User(
                "Vikram Batra",
                "vikram." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));

        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        DoctorAvailability slot1 = availabilityRepository.save(new DoctorAvailability(doctor1, baseTime, baseTime.plusHours(4), true));
        DoctorAvailability slot2 = availabilityRepository.save(new DoctorAvailability(doctor1, baseTime.plusDays(3), baseTime.plusDays(3).plusHours(4), true));

        // Create appointments for doctor1 and patient1
        app1 = appointmentRepository.save(new Appointment(
                patient1, doctor1, slot1, baseTime, baseTime.plusMinutes(30), AppointmentStatus.COMPLETED
        ));
        app2 = appointmentRepository.save(new Appointment(
                patient1, doctor1, slot2, baseTime.plusDays(3), baseTime.plusDays(3).plusMinutes(30), AppointmentStatus.COMPLETED
        ));

        // Create consultation records
        consult1 = consultationRepository.save(new ConsultationRecord(
                app1,
                "Patient presents with mild respiratory congestion and intermittent fever.",
                "Advised adequate hydration and rest.",
                LocalDate.now().plusDays(7),
                "Return if fever persists beyond 5 days"
        ));

        prescriptionRepository.save(new Prescription(
                app1, consult1, "Paracetamol", "500mg", "Twice daily after meals", "5 days"
        ));

        consult2 = consultationRepository.save(new ConsultationRecord(
                app2,
                "Follow-up visit: Congestion resolved, lungs clear on auscultation.",
                "Continue oral fluids, discharged from current episode.",
                null,
                null
        ));
    }

    @Test
    @DisplayName("1. Authorized doctor successfully requests summary")
    void authorizedDoctor_successfullyRequestsSummary() throws Exception {
        String mockAiOutput = """
### 1. Overview of Recorded Visits
Two recorded visits (2026-10-24 and 2026-10-27).
### 2. Previously Documented Concerns and Findings
Mild respiratory congestion and fever.
### 3. Recorded Treatment and Advice
Hydration, rest, and discharge on resolution.
### 4. Prescription History
Paracetamol 500mg twice daily for 5 days.
### 5. Follow-up History
Follow-up conducted on day 3; no further follow-up required.
### 6. Missing Information or Limitations
No allergies or past surgical history recorded.

AI-generated clinical summary — draft for doctor review. Verify against the original records. This is not a diagnosis or treatment recommendation.
""";
        when(aiClient.generateSummary(anyString(), anyString())).thenReturn(mockAiOutput);

        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", containsString("Overview of Recorded Visits")))
                .andExpect(jsonPath("$.draftDisclaimer", containsString("AI-generated clinical summary")))
                .andExpect(jsonPath("$.truncated", is(false)))
                .andExpect(jsonPath("$.consultationsIncluded", is(2)))
                .andExpect(jsonPath("$.totalEligibleConsultations", is(2)))
                .andExpect(jsonPath("$.generatedAt", notNullValue()));
    }

    @Test
    @DisplayName("2. Unauthenticated request is rejected with 401")
    void unauthenticatedRequest_rejectedWith401() throws Exception {
        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Patient role cannot invoke the endpoint (403)")
    void patientRole_cannotInvokeEndpoint() throws Exception {
        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Doctor cannot summarize a patient they are not authorized to access (403)")
    void doctorCannotSummarizeUnauthorizedPatient() throws Exception {
        // doctor2 has never consulted patient1
        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Empty history is handled safely (400 Bad Request)")
    void emptyHistory_handledSafely() throws Exception {
        // doctor1 has an appointment with patientNoHistory, but no consultation records exist
        LocalDateTime time = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
        DoctorAvailability slot = availabilityRepository.save(new DoctorAvailability(doctor1, time, time.plusHours(2), true));
        appointmentRepository.save(new Appointment(patientNoHistory, doctor1, slot, time, time.plusMinutes(30), AppointmentStatus.BOOKED));

        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patientNoHistory.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("No consultation history")));
    }

    @Test
    @DisplayName("6. Missing provider configuration produces clear error (503 Service Unavailable)")
    void missingProviderConfiguration_producesClearError() throws Exception {
        when(aiClient.generateSummary(anyString(), anyString()))
                .thenThrow(new AiConfigurationException("AI clinical summarizer is not configured."));

        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("AI clinical summarizer is not configured")));
    }

    @Test
    @DisplayName("7. Provider timeout or failure is handled without leaking internals (502 Bad Gateway)")
    void providerTimeoutOrFailure_handledSafely() throws Exception {
        when(aiClient.generateSummary(anyString(), anyString()))
                .thenThrow(new AiServiceException("AI service request timed out. Please try again later."));

        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message", containsString("timed out")));
    }

    @Test
    @DisplayName("8. Direct identifiers are excluded from the AI payload")
    void directIdentifiers_excludedFromAiPayload() throws Exception {
        when(aiClient.generateSummary(anyString(), anyString())).thenReturn("Draft summary");

        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> historyCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiClient).generateSummary(promptCaptor.capture(), historyCaptor.capture());

        String anonymizedPayload = historyCaptor.getValue();

        // Must NOT contain direct patient identity or account info
        assertFalse(anonymizedPayload.contains(patient1.getFullName()), "Payload must not contain patient full name");
        assertFalse(anonymizedPayload.contains(patient1.getEmail()), "Payload must not contain patient email");
        assertFalse(anonymizedPayload.contains(doctor1.getFullName()), "Payload must not contain doctor full name");
        assertFalse(anonymizedPayload.contains(doctor1.getEmail()), "Payload must not contain doctor email");

        // Must contain clinical notes, prescriptions, and follow-up
        assertTrue(anonymizedPayload.contains("mild respiratory congestion"), "Payload must contain clinical notes");
        assertTrue(anonymizedPayload.contains("Paracetamol"), "Payload must contain prescription medication");
        assertTrue(anonymizedPayload.contains("500mg"), "Payload must contain prescription dosage");
    }

    @Test
    @DisplayName("9. Generated text is not automatically saved to clinical records")
    void generatedText_notSavedToClinicalRecords() throws Exception {
        long initialConsultCount = consultationRepository.count();
        long initialRxCount = prescriptionRepository.count();

        when(aiClient.generateSummary(anyString(), anyString())).thenReturn("Draft AI summary that must not be persisted.");

        ClinicalHistorySummaryRequest req = new ClinicalHistorySummaryRequest(patient1.getId());

        mockMvc.perform(post("/api/v1/ai/clinical-history-summary")
                        .header("Authorization", "Bearer " + doctor1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Verify database counts and content remain unmodified
        org.junit.jupiter.api.Assertions.assertEquals(initialConsultCount, consultationRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(initialRxCount, prescriptionRepository.count());

        ConsultationRecord currentConsult1 = consultationRepository.findById(consult1.getId()).orElseThrow();
        assertFalse(currentConsult1.getNotes().contains("Draft AI summary"));
    }
}
