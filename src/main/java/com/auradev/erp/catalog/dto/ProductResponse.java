package com.auradev.erp.catalog.dto;

import com.auradev.erp.catalog.entity.StockStatus;
import com.auradev.erp.catalog.entity.UnitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Product with tenant inventory snapshot — schema v2.0 API response. */
public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String barcode,
        String categoryName,
        UUID categoryId,
        String unitLabel,
        UnitType unitType,
        BigDecimal priceMrp,
        BigDecimal priceSelling,
        BigDecimal costPrice,
        BigDecimal taxRatePct,
        BigDecimal quantityOnHand,
        BigDecimal lowStockThreshold,
        BigDecimal reorderQuantity,
        StockStatus stockStatus,
        boolean isActive,
        Instant createdAt
) {}
