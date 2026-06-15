package com.auradev.erp.importing.inventory.dto;

import java.math.BigDecimal;

public record StockAdjustmentImportRow(
        int rowNumber,
        String sku,
        String adjustment,
        BigDecimal quantity,
        String reason,
        String notes
) {}
