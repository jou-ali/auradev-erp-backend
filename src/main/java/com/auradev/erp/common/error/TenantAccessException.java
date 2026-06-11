package com.auradev.erp.common.error;

/**
 * Thrown when a principal attempts to access a resource that belongs to a
 * different tenant.
 *
 * <p>Mapped to HTTP 403 by {@link GlobalExceptionHandler}.</p>
 */
public class TenantAccessException extends RuntimeException {

    public TenantAccessException() {
        super("Access denied to tenant resource");
    }

    /**
     * Overload that allows callers to supply additional context without
     * changing the user-visible message.
     *
     * @param detail internal diagnostic detail (not exposed in the API response)
     */
    public TenantAccessException(String detail) {
        super("Access denied to tenant resource");
        // The detail string is intentionally not exposed to callers via the
        // public message; use getCause() or logging within the handler.
    }
}
