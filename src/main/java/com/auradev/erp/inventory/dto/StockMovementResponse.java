package com.auradev.erp.inventory.dto;

import com.auradev.erp.inventory.entity.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        String productName,
        String sku,
        MovementType movementType,
        BigDecimal quantity,
        BigDecimal signedDelta,
        UUID referenceId,
        BigDecimal quantityAfter,
        String notes,
        Instant createdAt
) {}
