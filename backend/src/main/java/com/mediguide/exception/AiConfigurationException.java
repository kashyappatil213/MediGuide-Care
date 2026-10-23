package com.mediguide.exception;

/**
 * Exception thrown when the AI provider is unconfigured (e.g. missing API key).
 */
public class AiConfigurationException extends RuntimeException {
    public AiConfigurationException(String message) {
        super(message);
    }
}
