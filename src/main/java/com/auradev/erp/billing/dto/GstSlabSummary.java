package com.auradev.erp.billing.dto;

import java.math.BigDecimal;

public record GstSlabSummary(
        BigDecimal ratePct,
        BigDecimal taxableValue,
        BigDecimal taxAmount
) {}
