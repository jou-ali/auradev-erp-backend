package com.auradev.erp.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
