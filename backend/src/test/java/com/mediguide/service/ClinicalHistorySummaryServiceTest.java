package com.mediguide.service;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ClinicalHistorySummaryResponse;
import com.mediguide.dto.ConsultationHistoryResponse;
import com.mediguide.dto.PrescriptionResponse;
import com.mediguide.model.AppointmentStatus;
import com.mediguide.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalHistorySummaryServiceTest {

    @Mock
    private ConsultationService consultationService;

    @Mock
    private ClinicalSummaryAiClient aiClient;

    @Test
    @DisplayName("Non-doctor principal is rejected")
    void nonDoctorPrincipal_throwsAccessDenied() {
        ClinicalHistorySummaryService service = new ClinicalHistorySummaryService(consultationService, aiClient, 5000);
        UserPrincipal patientPrincipal = new UserPrincipal(10L, "patient@test.com", Role.PATIENT);

        assertThrows(AccessDeniedException.class, () ->
                service.summarizePatientHistory(patientPrincipal, 10L));
    }

    @Test
    @DisplayName("Excessive clinical history is bounded and truncation is explicitly reported")
    void excessiveHistory_boundedAndTruncationReported() {
        // Set a small character budget of 600 characters
        ClinicalHistorySummaryService service = new ClinicalHistorySummaryService(consultationService, aiClient, 600);
        UserPrincipal doctorPrincipal = new UserPrincipal(1L, "dr@test.com", Role.DOCTOR);

        // Generate 5 consultations
        List<ConsultationHistoryResponse> history = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ConsultationHistoryResponse resp = new ConsultationHistoryResponse();
            resp.setConsultationId((long) i);
            resp.setAppointmentStartTime(LocalDateTime.now().minusDays(i));
            resp.setAppointmentStatus(AppointmentStatus.COMPLETED);
            resp.setNotes("Clinical consultation notes detailing symptoms and diagnosis for visit #" + i + ". Patient reported recurring symptoms.");
            resp.setTreatment("Prescribed rest, fluids, and standard symptomatic care for visit #" + i);
            resp.setFollowUpDate(LocalDate.now().plusDays(i));
            resp.setFollowUpNotes("Check progress in " + i + " days");
            resp.setPrescriptions(List.of(
                    new PrescriptionResponse(100L + i, (long) i, (long) i, "Amoxicillin", "500mg", "TID", "7 days", LocalDateTime.now())
            ));
            history.add(resp);
        }

        when(consultationService.getPatientHistoryForDoctor(doctorPrincipal, 50L)).thenReturn(history);
        when(aiClient.generateSummary(anyString(), anyString())).thenReturn("Truncated summary result");

        ClinicalHistorySummaryResponse response = service.summarizePatientHistory(doctorPrincipal, 50L);

        // History must be truncated because 5 full records exceed 600 characters
        assertTrue(response.isTruncated(), "Response must indicate that history was truncated");
        assertTrue(response.getConsultationsIncluded() < response.getTotalEligibleConsultations(),
                "Included records must be less than total records");
        assertEquals(5, response.getTotalEligibleConsultations());
        assertEquals("Truncated summary result", response.getSummary());
    }
}
