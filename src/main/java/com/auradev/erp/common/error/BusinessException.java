package com.auradev.erp.common.error;

import lombok.Getter;

/**
 * Thrown for domain-level business rule violations (e.g. oversell, invalid
 * state transitions, duplicate identifiers).
 *
 * <p>Mapped to HTTP 422 Unprocessable Content by {@link GlobalExceptionHandler}
 * so that clients can distinguish business rule failures from generic validation
 * errors while still treating them as "the request was well-formed but cannot
 * be processed".</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 *   throw new BusinessException("INSUFFICIENT_STOCK",
 *       "Requested quantity exceeds available stock for SKU " + sku);
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Machine-readable error code understood by API consumers.
     * Use UPPER_SNAKE_CASE, e.g. {@code "INSUFFICIENT_STOCK"}.
     */
    private final String code;

    /**
     * @param code    machine-readable error code (UPPER_SNAKE_CASE)
     * @param message human-readable description of the violation
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
    }
}
