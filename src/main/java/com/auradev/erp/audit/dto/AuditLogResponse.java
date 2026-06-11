package com.auradev.erp.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * API response payload representing a single audit log entry.
 *
 * @param id         surrogate UUID of the log entry
 * @param action     machine-readable action label, e.g. {@code BILL_CREATED}
 * @param userName   display name of the user who triggered the action;
 *                   {@code null} for system-generated events
 * @param entityType class name of the affected entity, e.g. {@code Bill}
 * @param entityId   UUID of the affected entity record
 * @param metadata   arbitrary event-specific context
 * @param ipAddress  originating client IP address
 * @param createdAt  UTC timestamp of the event
 */
public record AuditLogResponse(
        UUID id,
        String action,
        String userName,
        String entityType,
        UUID entityId,
        Map<String, Object> metadata,
        String ipAddress,
        Instant createdAt
) {}
