package com.auradev.erp.audit;

import java.util.Map;
import java.util.UUID;

/** Fired during a request; persisted asynchronously after the DB transaction commits. */
public record AuditLogEvent(
        UUID tenantId,
        UUID userId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> metadata,
        String ip) {}
