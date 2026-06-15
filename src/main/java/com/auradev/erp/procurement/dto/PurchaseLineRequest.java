package com.auradev.erp.procurement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseLineRequest(
        @NotNull UUID productId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal rate,
        BigDecimal gstRatePct
) {}
