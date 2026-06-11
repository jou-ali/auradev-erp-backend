package com.auradev.erp.catalog.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.auradev.erp.catalog.entity.Category}.
 *
 * @param id        category UUID
 * @param name      display name
 * @param sortOrder display position (ascending)
 * @param active    whether the category is currently active
 * @param createdAt creation timestamp (UTC)
 */
public record CategoryResponse(
        UUID id,
        String name,
        int sortOrder,
        boolean active,
        Instant createdAt
) {}
