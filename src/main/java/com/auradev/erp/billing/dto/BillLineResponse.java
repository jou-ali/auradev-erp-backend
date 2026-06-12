package com.auradev.erp.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineResponse(
        UUID productId,
        String name,
        String sku,
        String unitLabel,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal gstRate,
        BigDecimal lineTotal
) {}
