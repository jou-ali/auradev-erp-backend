package com.auradev.erp.inventory.dto;

import com.auradev.erp.inventory.entity.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductStockAdjustBody(
        @NotNull MovementType movementType,
        @NotNull @Positive BigDecimal quantity,
        String notes
) {}
