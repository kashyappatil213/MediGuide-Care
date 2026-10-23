package com.mediguide.exception;

/**
 * Exception thrown when the external AI provider fails, times out, rate limits, or returns an error.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
