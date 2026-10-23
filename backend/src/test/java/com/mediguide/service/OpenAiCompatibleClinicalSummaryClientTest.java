package com.mediguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.exception.AiConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleClinicalSummaryClientTest {

    @Test
    @DisplayName("Missing API key throws AiConfigurationException")
    void missingApiKey_throwsAiConfigurationException() {
        OpenAiCompatibleClinicalSummaryClient client = new OpenAiCompatibleClinicalSummaryClient(
                "",
                "https://api.openai.com/v1",
                "gpt-4o-mini",
                5,
                15,
                new ObjectMapper()
        );

        AiConfigurationException ex = assertThrows(AiConfigurationException.class, () ->
                client.generateSummary("system prompt", "clinical history"));

        assertTrue(ex.getMessage().contains("AI_API_KEY"));
    }
}
