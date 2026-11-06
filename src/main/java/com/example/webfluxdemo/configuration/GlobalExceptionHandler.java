package com.example.webfluxdemo.configuration;

import com.example.webfluxdemo.handler.ValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for WebFlux {@code RouterFunction}-based routing.
 * <p>
 * Implements {@link WebExceptionHandler} to intercept all unhandled exceptions
 * thrown by {@code RouterFunction} handlers and convert them into RFC 9457
 * {@link ProblemDetail} responses with {@code application/problem+json} content type.
 * <p>
 * Registered with the highest precedence to ensure it handles exceptions before
 * any default framework error handlers.
 *
 * @see WebExceptionHandler
 * @see ProblemDetail
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler implements WebExceptionHandler {

    /** Jackson mapper configured to serialize {@link ProblemDetail} with top-level extension properties. */
    private final JsonMapper objectMapper;

    /**
     * Constructs the handler with a Jackson mapper that serializes
     * {@link ProblemDetail} extension properties (e.g., {@code errors}) as
     * top-level JSON fields via {@link ProblemDetailJacksonMixin}.
     */
    public GlobalExceptionHandler() {
        this.objectMapper = JsonMapper.builder()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
    }

    /**
     * Handles an unhandled exception by converting it into an RFC 9457 {@link ProblemDetail} response.
     * <p>
     * Delegates to specific handling logic based on the exception type:
     * <ul>
     *     <li>{@link ResponseStatusException} — maps the status and reason; includes field-level
     *     {@code errors} when caused by a {@link ValidationException}.</li>
     *     <li>{@link ConstraintViolationException} — collects constraint violation messages into
     *     an {@code errors} map.</li>
     *     <li>{@link ServerWebInputException} — handles malformed input (e.g., invalid path variables).</li>
     *     <li>Any other {@link Throwable} — returns a generic 500 Internal Server Error.</li>
     * </ul>
     *
     * @param exchange the current server exchange providing request and response access
     * @param ex the unhandled exception
     * @return a {@link Mono} that writes the {@code ProblemDetail} JSON response
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ProblemDetail problem;
        HttpStatus status;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            problem = ProblemDetail.forStatusAndDetail(status, rse.getReason());
            problem.setTitle(rse.getReason());
            problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));

            if (rse.getCause() instanceof ValidationException validationEx) {
                problem.setType(URI.create("https://example.com/problems/validation-error"));
                problem.setProperty("errors", validationEx.getErrors());
            } else {
                problem.setType(typeForStatus(status));
            }
        } else if (ex instanceof ConstraintViolationException cve) {
            status = HttpStatus.BAD_REQUEST;
            problem = ProblemDetail.forStatusAndDetail(status, "Constraint violation");
            problem.setType(URI.create("https://example.com/problems/constraint-violation"));
            problem.setTitle("Constraint Violation");
            problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));

            Map<String, String> errors = new HashMap<>();
            cve.getConstraintViolations().forEach(violation ->
                    errors.put(violation.getPropertyPath().toString(), violation.getMessage())
            );
            problem.setProperty("errors", errors);
        } else if (ex instanceof ServerWebInputException swie) {
            status = HttpStatus.BAD_REQUEST;
            problem = ProblemDetail.forStatusAndDetail(status, "Bad Request");
            problem.setType(URI.create("https://example.com/problems/bad-request"));
            problem.setTitle("Bad Request");
            problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));
            problem.setDetail(swie.getMessage());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            problem = ProblemDetail.forStatusAndDetail(status, "Internal Server Error");
            problem.setType(URI.create("https://example.com/problems/internal-error"));
            problem.setTitle("Internal Server Error");
            problem.setInstance(URI.create(exchange.getRequest().getURI().getPath()));
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JacksonException e) {
            bytes = ("{\"status\":" + problem.getStatus() + "}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Maps an HTTP status code to an RFC 9457 {@code type} URI.
     * <p>
     * The {@code type} URI identifies the problem class and enables clients
     * to handle errors programmatically (e.g., retry logic for specific types).
     *
     * @param status the HTTP status code
     * @return a {@link URI} identifying the problem type, defaulting to
     * {@code https://example.com/problems/error} for unmapped statuses
     */
    private URI typeForStatus(HttpStatus status) {
        switch (status) {
            case NOT_FOUND:
                return URI.create("https://example.com/problems/not-found");
            case METHOD_NOT_ALLOWED:
                return URI.create("https://example.com/problems/method-not-allowed");
            case UNAUTHORIZED:
                return URI.create("https://example.com/problems/unauthorized");
            case FORBIDDEN:
                return URI.create("https://example.com/problems/forbidden");
            case CONFLICT:
                return URI.create("https://example.com/problems/conflict");
            case UNSUPPORTED_MEDIA_TYPE:
                return URI.create("https://example.com/problems/unsupported-media-type");
            case INTERNAL_SERVER_ERROR:
                return URI.create("https://example.com/problems/internal-error");
            default:
                return URI.create("https://example.com/problems/error");
        }
    }
}
