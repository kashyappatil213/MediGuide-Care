package com.mediguide.service;

import com.mediguide.config.UserPrincipal;
import com.mediguide.dto.ClinicalHistorySummaryResponse;
import com.mediguide.dto.ConsultationHistoryResponse;
import com.mediguide.dto.PrescriptionResponse;
import com.mediguide.exception.AppointmentValidationException;
import com.mediguide.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service orchestrating AI clinical history summarization.
 * Enforces doctor authorization, anonymizes records, bounds token/character budgets,
 * executes external AI drafting with strict medical safety rules, and returns temporary drafts.
 */
@Service
public class ClinicalHistorySummaryService {

    public static final String CLINICAL_SYSTEM_PROMPT = """
You are an expert AI clinical history summarizer assisting an attending physician.
Your role is to summarize the provided chronological clinical consultation records into a concise, structured review draft.

STRICT MEDICAL SAFETY RULES:
1. Ground every statement exclusively on the provided consultation records.
2. Do NOT diagnose medical conditions or hypothesize underlying etiologies.
3. Do NOT recommend new treatments, investigations, therapies, or medications.
4. Do NOT change, calculate, or infer medication dosages.
5. Do NOT infer drug allergies, medication adherence, or ongoing drug consumption.
6. A past prescription record is strictly historical evidence of a prescription; it is NOT proof that the patient currently takes the medicine.
7. Do NOT invent symptoms, findings, dates, or clinical facts.
8. For any section without documented evidence in the records, write exactly: "Not documented."

OUTPUT FORMAT:
Provide a concise, professional summary structured with the following exact Markdown headings:
### 1. Overview of Recorded Visits
### 2. Previously Documented Concerns and Findings
### 3. Recorded Treatment and Advice
### 4. Prescription History
### 5. Follow-up History
### 6. Missing Information or Limitations

End the summary with the exact disclaimer:
"AI-generated clinical summary — draft for doctor review. Verify against the original records. This is not a diagnosis or treatment recommendation."
""";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConsultationService consultationService;
    private final ClinicalSummaryAiClient aiClient;
    private final int maxInputChars;

    @Autowired
    public ClinicalHistorySummaryService(
            ConsultationService consultationService,
            ClinicalSummaryAiClient aiClient,
            @Value("${ai.summary.max-input-chars:6000}") int maxInputChars) {
        this.consultationService = consultationService;
        this.aiClient = aiClient;
        this.maxInputChars = maxInputChars > 500 ? maxInputChars : 6000;
    }

    /**
     * Generates an AI clinical history summary for a patient authorized for the requesting doctor.
     *
     * @param principal authenticated user principal (must be DOCTOR)
     * @param patientId ID of the patient to summarize
     * @return ClinicalHistorySummaryResponse draft
     */
    public ClinicalHistorySummaryResponse summarizePatientHistory(UserPrincipal principal, Long patientId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to summarize clinical history");
        }
        if (principal.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Only doctors are authorized to generate clinical history summaries");
        }
        if (patientId == null) {
            throw new AppointmentValidationException("Patient ID is required");
        }

        // Reuses existing relationship check and retrieval logic (throws AccessDeniedException if doctor is unauthorized)
        List<ConsultationHistoryResponse> history = consultationService.getPatientHistoryForDoctor(principal, patientId);

        if (history == null || history.isEmpty()) {
            throw new AppointmentValidationException("No consultation history recorded for this patient to summarize");
        }

        int totalEligible = history.size();
        StringBuilder contextBuilder = new StringBuilder();
        int includedCount = 0;
        boolean truncated = false;

        // Process records chronologically newest first
        for (int i = 0; i < history.size(); i++) {
            ConsultationHistoryResponse record = history.get(i);
            String formattedBlock = formatAnonymizedRecord(record, i + 1);

            if (contextBuilder.length() + formattedBlock.length() > maxInputChars) {
                if (includedCount == 0) {
                    // Truncate the first record if it exceeds the entire budget
                    int allowed = Math.max(200, maxInputChars - 100);
                    String partial = formattedBlock.length() > allowed
                            ? formattedBlock.substring(0, allowed) + "\n... [Record truncated to fit size limit]"
                            : formattedBlock;
                    contextBuilder.append(partial);
                    includedCount = 1;
                }
                truncated = true;
                break;
            } else {
                contextBuilder.append(formattedBlock).append("\n\n");
                includedCount++;
            }
        }

        if (includedCount < totalEligible) {
            truncated = true;
        }

        String summaryText = aiClient.generateSummary(CLINICAL_SYSTEM_PROMPT, contextBuilder.toString().trim());

        return new ClinicalHistorySummaryResponse(
                summaryText,
                ClinicalHistorySummaryResponse.DEFAULT_DRAFT_DISCLAIMER,
                truncated,
                includedCount,
                totalEligible,
                LocalDateTime.now()
        );
    }

    /**
     * Formats a consultation record into an anonymized clinical summary block.
     * Strips all direct patient and doctor identifiers, account info, and internal database keys.
     */
    public String formatAnonymizedRecord(ConsultationHistoryResponse r, int recordIndex) {
        StringBuilder sb = new StringBuilder();
        String visitTime = r.getAppointmentStartTime() != null
                ? r.getAppointmentStartTime().format(DATE_FORMATTER)
                : "Undated";

        sb.append("--- Consultation Record #").append(recordIndex)
          .append(" (Date: ").append(visitTime)
          .append(", Status: ").append(r.getAppointmentStatus() != null ? r.getAppointmentStatus() : "RECORDED")
          .append(") ---\n");

        sb.append("Clinical Notes & Findings: ")
          .append(r.getNotes() != null && !r.getNotes().isBlank() ? r.getNotes().trim() : "Not documented")
          .append("\n");

        sb.append("Treatment & Medical Advice: ")
          .append(r.getTreatment() != null && !r.getTreatment().isBlank() ? r.getTreatment().trim() : "Not documented")
          .append("\n");

        sb.append("Prescribed Medications:\n");
        if (r.getPrescriptions() != null && !r.getPrescriptions().isEmpty()) {
            for (PrescriptionResponse p : r.getPrescriptions()) {
                sb.append("  - ").append(p.getMedicationName())
                  .append(" | Dosage: ").append(p.getDosage() != null ? p.getDosage() : "Unspecified")
                  .append(" | Instructions: ").append(p.getInstructions() != null ? p.getInstructions() : "None")
                  .append(" | Duration: ").append(p.getDuration() != null ? p.getDuration() : "Unspecified")
                  .append("\n");
            }
        } else {
            sb.append("  None prescribed\n");
        }

        sb.append("Recommended Follow-up: ");
        if (r.getFollowUpDate() != null) {
            sb.append(r.getFollowUpDate());
            if (r.getFollowUpNotes() != null && !r.getFollowUpNotes().isBlank()) {
                sb.append(" (").append(r.getFollowUpNotes().trim()).append(")");
            }
            sb.append("\n");
        } else {
            sb.append("Not recommended\n");
        }

        return sb.toString().trim();
    }
}
