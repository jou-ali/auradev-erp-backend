package com.auradev.erp.importing.inventory.dto;

import com.auradev.erp.catalog.entity.UnitType;

import java.math.BigDecimal;

public record InventoryImportRow(
        int rowNumber,
        String name,
        String sku,
        String barcode,
        String categoryName,
        UnitType unitType,
        String unitLabel,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        BigDecimal costPrice,
        BigDecimal taxRatePct,
        BigDecimal initialStock,
        BigDecimal reorderLevel
) {}
