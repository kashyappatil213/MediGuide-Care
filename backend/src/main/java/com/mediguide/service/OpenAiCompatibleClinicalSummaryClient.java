package com.mediguide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediguide.exception.AiConfigurationException;
import com.mediguide.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production implementation of ClinicalSummaryAiClient using standard HTTP REST
 * against an OpenAI-compatible chat completions endpoint (e.g. OpenAI, Azure OpenAI, Groq, local models).
 */
@Service
public class OpenAiCompatibleClinicalSummaryClient implements ClinicalSummaryAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClinicalSummaryClient.class);

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleClinicalSummaryClient(
            @Value("${ai.provider.api-key:}") String apiKey,
            @Value("${ai.provider.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.provider.model:gpt-4o-mini}") String model,
            @Value("${ai.provider.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${ai.provider.read-timeout-seconds:15}") int readTimeoutSeconds,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://api.openai.com/v1";
        this.model = model != null && !model.isBlank() ? model.trim() : "gpt-4o-mini";
        this.connectTimeoutSeconds = connectTimeoutSeconds > 0 ? connectTimeoutSeconds : 5;
        this.readTimeoutSeconds = readTimeoutSeconds > 0 ? readTimeoutSeconds : 15;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateSummary(String systemPrompt, String anonymizedClinicalHistory) {
        if (apiKey.isEmpty()) {
            throw new AiConfigurationException(
                    "AI clinical summarizer is not configured. Please supply an AI_API_KEY environment variable."
            );
        }

        if (apiKey.startsWith("demo") || apiKey.startsWith("mock") || apiKey.equalsIgnoreCase("local") || apiKey.equalsIgnoreCase("offline")) {
            log.info("Generating structured clinical history summary (demo/local engine active)");
            return generateLocalClinicalSummary(anonymizedClinicalHistory);
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", model);
        requestPayload.put("temperature", 0.2);
        requestPayload.put("max_tokens", 1500);
        requestPayload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", anonymizedClinicalHistory)
        ));

        try {
            log.info("Dispatching clinical history summary request to AI provider (model: {})", model);

            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
                throw new AiServiceException("AI provider returned an empty or malformed completion response.");
            }

            JsonNode firstChoice = response.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                String content = firstChoice.get("message").get("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.trim();
                }
            }

            throw new AiServiceException("AI provider returned completion without readable message content.");
        } catch (ResourceAccessException ex) {
            log.error("AI provider connection or read timeout after {}s connect / {}s read",
                    connectTimeoutSeconds, readTimeoutSeconds);
            throw new AiServiceException("AI service request timed out. Please try again later.", ex);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            log.error("AI provider returned authentication/authorization failure ({})", ex.getStatusCode());
            throw new AiServiceException("AI provider authentication failed. Please verify provider credentials.", ex);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI provider rate limit reached (HTTP 429)");
            throw new AiServiceException("AI service rate limit exceeded. Please try again shortly.", ex);
        } catch (HttpServerErrorException ex) {
            log.error("AI provider server error (HTTP {})", ex.getStatusCode());
            throw new AiServiceException("AI provider service is temporarily unavailable. Please try again later.", ex);
        } catch (RestClientResponseException ex) {
            log.error("AI provider returned HTTP client error ({})", ex.getStatusCode());
            throw new AiServiceException("AI summary generation failed with HTTP status " + ex.getStatusCode().value(), ex);
        } catch (AiConfigurationException | AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error communicating with AI provider: {}", ex.getClass().getSimpleName());
            throw new AiServiceException("AI summary generation encountered an unexpected internal communication error.", ex);
        }
    }

    /**
     * Synthesizes an intelligent, structured clinical summary directly from documented records
     * when in demo, local, or mock API key mode.
     */
    private String generateLocalClinicalSummary(String history) {
        if (history == null || history.isBlank()) {
            return "### 1. Overview of Recorded Visits\nNo consultation history documented.\n\n" +
                   "### 2. Previously Documented Concerns and Findings\nNot documented.\n\n" +
                   "### 3. Recorded Treatment and Advice\nNot documented.\n\n" +
                   "### 4. Prescription History\nNone prescribed.\n\n" +
                   "### 5. Follow-up History\nNot documented.\n\n" +
                   "### 6. Missing Information or Limitations\nNo clinical data available for review.\n\n" +
                   "AI-generated clinical summary — draft for doctor review. Verify against the original records. This is not a diagnosis or treatment recommendation.";
        }

        List<String> visitDates = new ArrayList<>();
        List<String> notesList = new ArrayList<>();
        List<String> treatmentsList = new ArrayList<>();
        List<String> prescriptionsList = new ArrayList<>();
        List<String> followUpsList = new ArrayList<>();

        String[] lines = history.split("\\r?\\n");
        String currentSection = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("--- Consultation Record")) {
                int dateIdx = trimmed.indexOf("Date: ");
                if (dateIdx != -1) {
                    int commaIdx = trimmed.indexOf(",", dateIdx);
                    String date = commaIdx != -1
                            ? trimmed.substring(dateIdx + 6, commaIdx).trim()
                            : trimmed.substring(dateIdx + 6, trimmed.length() - 4).trim();
                    visitDates.add(date);
                }
                currentSection = "RECORD";
            } else if (trimmed.startsWith("Clinical Notes & Findings:")) {
                String val = trimmed.substring("Clinical Notes & Findings:".length()).trim();
                if (!val.equalsIgnoreCase("Not documented")) {
                    notesList.add(val);
                }
                currentSection = "NOTES";
            } else if (trimmed.startsWith("Treatment & Medical Advice:")) {
                String val = trimmed.substring("Treatment & Medical Advice:".length()).trim();
                if (!val.equalsIgnoreCase("Not documented")) {
                    treatmentsList.add(val);
                }
                currentSection = "TREATMENT";
            } else if (trimmed.startsWith("Prescribed Medications:")) {
                currentSection = "PRESCRIPTIONS";
            } else if (trimmed.startsWith("Recommended Follow-up:")) {
                String val = trimmed.substring("Recommended Follow-up:".length()).trim();
                if (!val.equalsIgnoreCase("None scheduled") && !val.equalsIgnoreCase("Not documented")) {
                    followUpsList.add(val);
                }
                currentSection = "FOLLOWUP";
            } else if (currentSection.equals("PRESCRIPTIONS") && trimmed.startsWith("-")) {
                if (!trimmed.contains("None prescribed")) {
                    prescriptionsList.add(trimmed.substring(1).trim());
                }
            } else if (currentSection.equals("NOTES") && !trimmed.startsWith("Treatment &") && !trimmed.startsWith("Prescribed") && !trimmed.startsWith("Recommended")) {
                notesList.add(trimmed);
            } else if (currentSection.equals("TREATMENT") && !trimmed.startsWith("Prescribed") && !trimmed.startsWith("Recommended")) {
                treatmentsList.add(trimmed);
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("### 1. Overview of Recorded Visits\n");
        int count = Math.max(visitDates.size(), 1);
        sb.append("- Documented Consultations: ").append(count).append(" recorded visit(s).\n");
        if (!visitDates.isEmpty()) {
            sb.append("- Consultation Dates: ").append(String.join(", ", visitDates)).append(".\n\n");
        } else {
            sb.append("- Consultation Dates: Documented chronological visits.\n\n");
        }

        sb.append("### 2. Previously Documented Concerns and Findings\n");
        if (!notesList.isEmpty()) {
            for (String note : notesList) {
                sb.append("- ").append(note).append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("Not documented.\n\n");
        }

        sb.append("### 3. Recorded Treatment and Advice\n");
        if (!treatmentsList.isEmpty()) {
            for (String treatment : treatmentsList) {
                sb.append("- ").append(treatment).append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("Not documented.\n\n");
        }

        sb.append("### 4. Prescription History\n");
        if (!prescriptionsList.isEmpty()) {
            for (String rx : prescriptionsList) {
                sb.append("- ").append(rx).append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("None prescribed in documented visits.\n\n");
        }

        sb.append("### 5. Follow-up History\n");
        if (!followUpsList.isEmpty()) {
            for (String f : followUpsList) {
                sb.append("- Scheduled: ").append(f).append("\n");
            }
            sb.append("\n");
        } else {
            sb.append("No active follow-up visits recorded.\n\n");
        }

        sb.append("### 6. Missing Information or Limitations\n");
        sb.append("- Baseline vital signs (blood pressure, temperature, heart rate), laboratory diagnostic reports, and documented drug allergies were not recorded in the reviewed consultation history.\n\n");

        sb.append("AI-generated clinical summary — draft for doctor review. Verify against the original records. This is not a diagnosis or treatment recommendation.");

        return sb.toString();
    }
}
