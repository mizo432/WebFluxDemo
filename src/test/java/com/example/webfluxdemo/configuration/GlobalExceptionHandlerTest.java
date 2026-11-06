package com.example.webfluxdemo.configuration;

import com.example.webfluxdemo.handler.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationErrors_returnsProblemDetailWithFieldErrors() {
        // Given
        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        org.springframework.validation.FieldError fieldError1 = mock(org.springframework.validation.FieldError.class);
        org.springframework.validation.FieldError fieldError2 = mock(org.springframework.validation.FieldError.class);

        when(fieldError1.getField()).thenReturn("name");
        when(fieldError1.getDefaultMessage()).thenReturn("must not be blank");
        when(fieldError2.getField()).thenReturn("email");
        when(fieldError2.getDefaultMessage()).thenReturn("must be a valid email");
        when(ex.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // When
        ResponseEntity<ProblemDetail> response = handler.handleValidationErrors(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals(URI.create("https://example.com/problems/validation-error"), problem.getType());
        assertEquals("Validation Failed", problem.getTitle());
        assertEquals(Integer.valueOf(HttpStatus.BAD_REQUEST.value()), problem.getStatus());
        assertEquals("Validation failed", problem.getDetail());

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals("must not be blank", errors.get("name"));
        assertEquals("must be a valid email", errors.get("email"));
    }

    @Test
    void handleResponseStatus_withValidationException_returnsProblemDetailWithErrors() {
        // Given
        Map<String, String> validationErrors = new HashMap<>();
        validationErrors.put("name", "名前は必須です");
        validationErrors.put("email", "メールは必須です");

        ValidationException validationEx = new ValidationException(validationErrors);
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Validation failed", validationEx);

        // When
        ResponseEntity<ProblemDetail> response = handler.handleResponseStatus(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals(URI.create("https://example.com/problems/validation-error"), problem.getType());
        assertEquals("Validation failed", problem.getTitle());
        assertEquals(Integer.valueOf(HttpStatus.BAD_REQUEST.value()), problem.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals("名前は必須です", errors.get("name"));
        assertEquals("メールは必須です", errors.get("email"));
    }

    @Test
    void handleResponseStatus_withoutValidationException_returnsProblemDetailWithoutErrors() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found");

        // When
        ResponseEntity<ProblemDetail> response = handler.handleResponseStatus(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals("User not found", problem.getTitle());
        assertEquals(Integer.valueOf(HttpStatus.NOT_FOUND.value()), problem.getStatus());
        Map<String, Object> properties = problem.getProperties();
        assertNull(properties);
    }

    @Test
    void handleConstraintViolation_returnsProblemDetailWithPropertyErrors() {
        // Given
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);

        when(violation1.getPropertyPath()).thenReturn(new PropertyPath("name"));
        when(violation1.getMessage()).thenReturn("must not be blank");
        when(violation2.getPropertyPath()).thenReturn(new PropertyPath("email"));
        when(violation2.getMessage()).thenReturn("must be a valid email");

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation1);
        violations.add(violation2);

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        // When
        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(ex);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ProblemDetail problem = response.getBody();
        assertNotNull(problem);
        assertEquals(URI.create("https://example.com/problems/constraint-violation"), problem.getType());
        assertEquals("Constraint Violation", problem.getTitle());
        assertEquals(Integer.valueOf(HttpStatus.BAD_REQUEST.value()), problem.getStatus());
        assertEquals("Constraint violation", problem.getDetail());

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals("must not be blank", errors.get("name"));
        assertEquals("must be a valid email", errors.get("email"));
    }

    // Helper class to mock jakarta.validation.Path
    static class PropertyPath implements jakarta.validation.Path {
        private final String name;

        PropertyPath(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public Iterator<jakarta.validation.Path.Node> iterator() {
            return Collections.emptyIterator();
        }
    }
}
