package com.auradev.erp.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        UUID parentId,
        boolean isActive,
        Instant createdAt
) {}
