package com.auradev.erp.catalog.dto;

import com.auradev.erp.catalog.entity.UnitType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        String barcode,
        @NotBlank String categoryId,
        String supplierId,
        UnitType unitType,
        @NotBlank String unitLabel,
        @NotNull @Positive BigDecimal priceMrp,
        @NotNull @Positive BigDecimal priceSelling,
        BigDecimal costPrice,
        @NotNull @DecimalMin("0") BigDecimal taxRatePct,
        @PositiveOrZero BigDecimal lowStockThreshold,
        @PositiveOrZero BigDecimal reorderQuantity
) {}
