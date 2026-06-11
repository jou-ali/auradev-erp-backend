package com.auradev.erp.audit.service;

import java.util.UUID;

/**
 * Contract for writing business-event audit log entries to the
 * {@code audit_log} table.
 *
 * <p>Implementations should be non-throwing: a failure to record an audit
 * entry must never roll back the surrounding business transaction.  Use
 * {@code @Async} + a separate transaction ({@code REQUIRES_NEW}) or an
 * out-of-band mechanism such as a message queue.</p>
 */
public interface AuditService {

    /**
     * Record a business event.
     *
     * @param tenantId   the tenant in whose context the action occurred;
     *                   {@code null} for SUPER_ADMIN cross-tenant actions
     * @param userId     the user who triggered the action;
     *                   {@code null} for system-generated events
     * @param action     a short machine-readable label, e.g. {@code "LOGIN_SUCCESS"}
     * @param entityType the class of the affected entity, e.g. {@code "User"};
     *                   {@code null} if not entity-specific
     * @param entityId   the UUID of the affected entity; {@code null} if not applicable
     * @param ipAddress  the originating IP address; {@code null} if unavailable
     */
    void log(
            UUID tenantId,
            UUID userId,
            String action,
            String entityType,
            UUID entityId,
            String ipAddress
    );
}
