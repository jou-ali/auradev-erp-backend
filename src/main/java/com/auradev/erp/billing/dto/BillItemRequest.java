package com.auradev.erp.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line-item entry inside a {@link CreateBillRequest}.
 *
 * @param productId    the product to sell
 * @param quantity     quantity sold (must be positive)
 * @param unitPrice    override unit price; if null the product's {@code sellingPrice} is used
 * @param lineDiscount absolute discount applied to this line before tax
 */
public record BillItemRequest(

        @NotNull
        UUID productId,

        @NotNull
        @DecimalMin(value = "0.001", message = "quantity must be greater than 0")
        BigDecimal quantity,

        BigDecimal unitPrice,

        BigDecimal lineDiscount
) {}
