package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockRow(
        UUID id,
        String name,
        String sku,
        String category,
        String unitLabel,
        BigDecimal stock,
        BigDecimal reorder,
        String status
) {}
