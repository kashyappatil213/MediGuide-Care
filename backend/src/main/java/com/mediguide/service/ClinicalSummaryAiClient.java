package com.mediguide.service;

/**
 * Interface defining the external AI client for generating clinical history summaries.
 * Allows easy swapping and mocking in automated tests without real external API calls.
 */
public interface ClinicalSummaryAiClient {

    /**
     * Generates a clinical history summary using an external AI provider.
     *
     * @param systemPrompt clinical instructions, rules, and safety boundaries
     * @param anonymizedClinicalHistory stripped chronological consultation history
     * @return AI-generated structured summary draft
     */
    String generateSummary(String systemPrompt, String anonymizedClinicalHistory);
}
