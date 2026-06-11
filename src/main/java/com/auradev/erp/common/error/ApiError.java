package com.auradev.erp.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * RFC 7807 "Problem Details for HTTP APIs" response body.
 *
 * <p>Serialised with {@code Content-Type: application/problem+json}.</p>
 *
 * <p>Null / empty collections are omitted from the JSON output to keep
 * responses concise when there are no field-level validation errors.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /**
     * A URI reference that identifies the problem type.
     * Example: {@code "https://erp.auradev.com/problems/validation-error"}
     */
    private String type;

    /**
     * A short, human-readable summary of the problem type.
     * Example: {@code "Validation Failed"}
     */
    private String title;

    /**
     * The HTTP status code for this occurrence of the problem.
     */
    private int status;

    /**
     * A human-readable explanation specific to this occurrence of the problem.
     */
    private String detail;

    /**
     * Machine-readable error code understood by the client.
     * Example: {@code "VALIDATION_ERROR"}, {@code "CONFLICT"}, {@code "VERSION_CONFLICT"}
     */
    private String code;

    /**
     * A URI reference that identifies the specific occurrence of the problem.
     * Typically the request path.  Example: {@code "/api/v1/products/123"}
     */
    private String instance;

    /**
     * The instant at which the error was generated (server time, UTC).
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Field-level validation errors; present only when the problem is a
     * validation failure (HTTP 422).
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<FieldError> errors;

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Describes a single field-level validation failure.
     *
     * @param field   the name of the offending request field (dot-notation for
     *                nested fields, e.g. {@code "address.postalCode"})
     * @param message the human-readable rejection reason
     */
    public record FieldError(String field, String message) {
    }
}
