package com.mediguide.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ConsultationHistoryController.
 * Validates patient and doctor consultation history retrieval,
 * chronological ordering, doctor-patient privacy boundaries, and error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsultationHistoryControllerTest {

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
    private User patientWithoutHistory;

    private String doctor1Token;
    private String doctor2Token;
    private String patient1Token;
    private String patient2Token;
    private String patientNoHistoryToken;

    private Appointment app1Older;
    private Appointment app2Newer;
    private Appointment app3Patient2;

    private ConsultationRecord consult1;
    private ConsultationRecord consult2;
    private ConsultationRecord consult3;

    @BeforeEach
    void setUp() {
        doctor1 = userRepository.save(new User(
                "Dr. Vikram Sarabhai",
                "dr.vikram." + System.nanoTime() + "@mediguide.local",
                "DoctorPass123",
                Role.DOCTOR
        ));
        doctor1Token = jwtService.generateToken(doctor1);

        doctor2 = userRepository.save(new User(
                "Dr. Homi Bhabha",
                "dr.homi." + System.nanoTime() + "@mediguide.local",
                "DoctorPass123",
                Role.DOCTOR
        ));
        doctor2Token = jwtService.generateToken(doctor2);

        patient1 = userRepository.save(new User(
                "Aarav Patel",
                "aarav." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patient1Token = jwtService.generateToken(patient1);

        patient2 = userRepository.save(new User(
                "Diya Sengupta",
                "diya." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patient2Token = jwtService.generateToken(patient2);

        patientWithoutHistory = userRepository.save(new User(
                "Karan Johar",
                "karan." + System.nanoTime() + "@mediguide.local",
                "PatientPass123",
                Role.PATIENT
        ));
        patientNoHistoryToken = jwtService.generateToken(patientWithoutHistory);

        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        DoctorAvailability slot1 = availabilityRepository.save(new DoctorAvailability(doctor1, baseTime, baseTime.plusHours(4), true));
        DoctorAvailability slot2 = availabilityRepository.save(new DoctorAvailability(doctor1, baseTime.plusDays(5), baseTime.plusDays(5).plusHours(4), true));
        DoctorAvailability slot3 = availabilityRepository.save(new DoctorAvailability(doctor2, baseTime, baseTime.plusHours(4), true));

        // Appointment 1: Patient 1 with Doctor 1 (Older appointment)
        app1Older = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                slot1,
                baseTime,
                baseTime.plusMinutes(30),
                AppointmentStatus.COMPLETED
        ));

        // Appointment 2: Patient 1 with Doctor 1 (Newer appointment)
        app2Newer = appointmentRepository.save(new Appointment(
                patient1,
                doctor1,
                slot2,
                baseTime.plusDays(5),
                baseTime.plusDays(5).plusMinutes(30),
                AppointmentStatus.COMPLETED
        ));

        // Appointment 3: Patient 2 with Doctor 2
        app3Patient2 = appointmentRepository.save(new Appointment(
                patient2,
                doctor2,
                slot3,
                baseTime,
                baseTime.plusMinutes(30),
                AppointmentStatus.COMPLETED
        ));

        // Consultation 1: on Older Appointment, includes follow-up and prescription
        consult1 = consultationRepository.save(new ConsultationRecord(
                app1Older,
                "Acute pharyngitis diagnosed.",
                "Warm saline gargles and voice rest.",
                LocalDate.now().plusWeeks(1),
                "Check throat inflammation."
        ));

        Prescription rx1 = prescriptionRepository.save(new Prescription(
                app1Older,
                consult1,
                "Azithromycin 500mg",
                "1 tablet daily",
                "Take after lunch",
                "3 days"
        ));

        // Consultation 2: on Newer Appointment, no prescriptions and no follow-up
        consult2 = consultationRepository.save(new ConsultationRecord(
                app2Newer,
                "Follow-up visit: Throat inflammation fully resolved.",
                "Maintain hydration.",
                null,
                null
        ));

        // Consultation 3: Patient 2 with Doctor 2
        consult3 = consultationRepository.save(new ConsultationRecord(
                app3Patient2,
                "Hypertension initial screening.",
                "Low sodium diet and exercise.",
                LocalDate.now().plusMonths(1),
                "Blood pressure check."
        ));
    }

    @Test
    @DisplayName("1. Patient retrieves their own consultation history via role-aware endpoint: 200 OK")
    void shouldAllowPatientToRetrieveTheirOwnConsultationHistory() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history")
                        .header("Authorization", "Bearer " + patient1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].consultationId", is(consult2.getId().intValue())))
                .andExpect(jsonPath("$[0].appointmentId", is(app2Newer.getId().intValue())))
                .andExpect(jsonPath("$[0].doctorName", is(doctor1.getFullName())))
                .andExpect(jsonPath("$[0].notes", is("Follow-up visit: Throat inflammation fully resolved.")))
                .andExpect(jsonPath("$[0].treatment", is("Maintain hydration.")))
                .andExpect(jsonPath("$[0].prescriptions", empty()))
                .andExpect(jsonPath("$[0].followUpDate", nullValue()))
                .andExpect(jsonPath("$[1].consultationId", is(consult1.getId().intValue())))
                .andExpect(jsonPath("$[1].appointmentId", is(app1Older.getId().intValue())))
                .andExpect(jsonPath("$[1].prescriptions", hasSize(1)))
                .andExpect(jsonPath("$[1].prescriptions[0].medicationName", is("Azithromycin 500mg")))
                .andExpect(jsonPath("$[1].followUpDate", is(LocalDate.now().plusWeeks(1).toString())))
                .andExpect(jsonPath("$[1].followUpNotes", is("Check throat inflammation.")));
    }

    @Test
    @DisplayName("2. Patient can use explicit /patient history endpoint: 200 OK")
    void shouldAllowPatientToUseExplicitPatientEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("3. Patient cannot retrieve another patient's history: 403 Forbidden")
    void shouldDenyPatientFromRetrievingAnotherPatientHistory() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient/" + patient2.getId())
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Doctor retrieves their own conducted consultation history: 200 OK")
    void shouldAllowDoctorToRetrieveConductedConsultationHistory() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].doctorId", is(doctor1.getId().intValue())))
                .andExpect(jsonPath("$[1].doctorId", is(doctor1.getId().intValue())));
    }

    @Test
    @DisplayName("5. Doctor can retrieve history of a patient they have consulted: 200 OK")
    void shouldAllowDoctorToRetrieveHistoryOfConsultedPatient() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient/" + patient1.getId())
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].patientId", is(patient1.getId().intValue())))
                .andExpect(jsonPath("$[1].patientId", is(patient1.getId().intValue())));
    }

    @Test
    @DisplayName("6. Unrelated doctor access to patient history is denied: 403 Forbidden")
    void shouldDenyUnrelatedDoctorFromAccessingPatientHistory() throws Exception {
        // Doctor 2 has never consulted Patient 1
        mockMvc.perform(get("/api/v1/consultations/history/patient/" + patient1.getId())
                        .header("Authorization", "Bearer " + doctor2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Chronological ordering: results are strictly ordered newest first")
    void shouldReturnConsultationsOrderedNewestFirst() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].appointmentId", is(app2Newer.getId().intValue())))
                .andExpect(jsonPath("$[0].consultationId", is(consult2.getId().intValue())))
                .andExpect(jsonPath("$[1].appointmentId", is(app1Older.getId().intValue())))
                .andExpect(jsonPath("$[1].consultationId", is(consult1.getId().intValue())));
    }

    @Test
    @DisplayName("8. Records without prescriptions or follow-up are handled cleanly")
    void shouldHandleRecordsWithoutPrescriptionsOrFollowUp() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prescriptions", empty()))
                .andExpect(jsonPath("$[0].followUpDate", nullValue()))
                .andExpect(jsonPath("$[0].followUpNotes", nullValue()));
    }

    @Test
    @DisplayName("9. Patient with no consultation history returns empty list: 200 OK")
    void shouldReturnEmptyListForPatientWithoutHistory() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient")
                        .header("Authorization", "Bearer " + patientNoHistoryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("10. Unauthenticated requests are rejected: 401 Unauthorized")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/consultations/history/patient"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/consultations/history/doctor"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/consultations/history/patient/" + patient1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Doctor role cannot call patient-only /patient endpoint: 403 Forbidden")
    void shouldDenyDoctorFromAccessingPatientSpecificEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/patient")
                        .header("Authorization", "Bearer " + doctor1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Patient role cannot call doctor-only /doctor endpoint: 403 Forbidden")
    void shouldDenyPatientFromAccessingDoctorSpecificEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/history/doctor")
                        .header("Authorization", "Bearer " + patient1Token))
                .andExpect(status().isForbidden());
    }
}
