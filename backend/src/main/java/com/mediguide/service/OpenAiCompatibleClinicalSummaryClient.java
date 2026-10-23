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
}
