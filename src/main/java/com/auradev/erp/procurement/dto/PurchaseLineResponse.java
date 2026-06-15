package com.auradev.erp.procurement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseLineResponse(
        UUID productId,
        String name,
        String sku,
        String unitLabel,
        BigDecimal quantity,
        BigDecimal rate,
        BigDecimal gstRate,
        BigDecimal amount,
        BigDecimal gstAmount,
        BigDecimal lineTotal
) {}
