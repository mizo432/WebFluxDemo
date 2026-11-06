package com.example.webfluxdemo.handler;

import java.util.Collections;
import java.util.Map;

/**
 * Exception carrying field-level validation errors.
 * <p>
 * Thrown when request validation fails, wrapping a map of field names to
 * error messages. The errors map is immutable to prevent external modification.
 * <p>
 * Typically used as the cause of {@link org.springframework.web.server.ResponseStatusException}
 * so that {@link com.example.webfluxdemo.configuration.GlobalExceptionHandler} can extract
 * the field-level errors and include them in the RFC 9457 {@code errors} property.
 *
 * @see com.example.webfluxdemo.configuration.GlobalExceptionHandler
 */
public class ValidationException extends Exception {

    /** Immutable map of field names to their corresponding error messages. */
    private final Map<String, String> errors;

    /**
     * Creates a new validation exception with the given field errors.
     *
     * @param errors a map where keys are field names and values are error messages;
     *               the map is copied and wrapped as immutable
     */
    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = Collections.unmodifiableMap(new java.util.HashMap<>(errors));
    }

    /**
     * Returns the immutable map of field-level validation errors.
     *
     * @return an unmodifiable map of field names to error messages
     */
    public Map<String, String> getErrors() {
        return errors;
    }
}
