package com.mediguide.dto;

import java.time.LocalDateTime;

/**
 * Response DTO returning an AI-generated clinical history summary draft for doctor review.
 */
public class ClinicalHistorySummaryResponse {

    public static final String DEFAULT_DRAFT_DISCLAIMER =
            "AI-generated clinical summary \u2014 draft for doctor review. Verify against the original records. This is not a diagnosis or treatment recommendation.";

    private String summary;
    private String draftDisclaimer;
    private boolean truncated;
    private int consultationsIncluded;
    private int totalEligibleConsultations;
    private LocalDateTime generatedAt;

    public ClinicalHistorySummaryResponse() {
        this.draftDisclaimer = DEFAULT_DRAFT_DISCLAIMER;
        this.generatedAt = LocalDateTime.now();
    }

    public ClinicalHistorySummaryResponse(String summary, boolean truncated,
                                          int consultationsIncluded, int totalEligibleConsultations) {
        this.summary = summary;
        this.draftDisclaimer = DEFAULT_DRAFT_DISCLAIMER;
        this.truncated = truncated;
        this.consultationsIncluded = consultationsIncluded;
        this.totalEligibleConsultations = totalEligibleConsultations;
        this.generatedAt = LocalDateTime.now();
    }

    public ClinicalHistorySummaryResponse(String summary, String draftDisclaimer, boolean truncated,
                                          int consultationsIncluded, int totalEligibleConsultations,
                                          LocalDateTime generatedAt) {
        this.summary = summary;
        this.draftDisclaimer = draftDisclaimer != null ? draftDisclaimer : DEFAULT_DRAFT_DISCLAIMER;
        this.truncated = truncated;
        this.consultationsIncluded = consultationsIncluded;
        this.totalEligibleConsultations = totalEligibleConsultations;
        this.generatedAt = generatedAt != null ? generatedAt : LocalDateTime.now();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDraftDisclaimer() {
        return draftDisclaimer;
    }

    public void setDraftDisclaimer(String draftDisclaimer) {
        this.draftDisclaimer = draftDisclaimer;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public int getConsultationsIncluded() {
        return consultationsIncluded;
    }

    public void setConsultationsIncluded(int consultationsIncluded) {
        this.consultationsIncluded = consultationsIncluded;
    }

    public int getTotalEligibleConsultations() {
        return totalEligibleConsultations;
    }

    public void setTotalEligibleConsultations(int totalEligibleConsultations) {
        this.totalEligibleConsultations = totalEligibleConsultations;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
