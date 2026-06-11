package com.auradev.erp.inventory.dto;

import com.auradev.erp.inventory.entity.MovementReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.auradev.erp.inventory.entity.StockMovement}.
 *
 * @param id           movement UUID
 * @param productName  snapshot of the product's display name at the time of
 *                     query (read from the joined product row)
 * @param sku          product SKU for quick identification
 * @param delta        signed stock change (positive = in, negative = out)
 * @param reason       business reason for the movement
 * @param referenceId  UUID of the originating document; may be {@code null}
 *                     for ad-hoc adjustments
 * @param balanceAfter running stock balance immediately after this movement
 * @param notes        optional free-text note
 * @param createdAt    UTC timestamp when the movement was recorded
 */
public record StockMovementResponse(
        UUID id,
        String productName,
        String sku,
        BigDecimal delta,
        MovementReason reason,
        UUID referenceId,
        BigDecimal balanceAfter,
        String notes,
        Instant createdAt
) {
}
