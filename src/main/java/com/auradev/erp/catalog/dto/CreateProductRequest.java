package com.auradev.erp.catalog.dto;

import com.auradev.erp.catalog.entity.ProductUnit;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request body for creating or fully replacing a product.
 *
 * <p>Used by both POST (create) and PUT (update) endpoints in
 * {@code CatalogController}.  The service layer applies additional
 * cross-field validations (SKU/barcode uniqueness per tenant, excluding the
 * product being updated).</p>
 *
 * @param name          display name; required, non-blank
 * @param sku           stock-keeping unit; required, non-blank, unique per tenant
 * @param barcode       optional barcode value; unique per tenant when provided
 * @param categoryId    UUID of the parent category as a String; required
 * @param unit          unit of measure; defaults to PCS if omitted
 * @param mrp           maximum retail price; must be positive
 * @param sellingPrice  selling price; must be positive
 * @param costPrice     landed/purchase cost; nullable (may not be known at creation)
 * @param gstRate       GST rate percentage; must be >= 0 (zero-rated products allowed)
 * @param hsnCode       HSN/SAC code; nullable
 * @param currentStock  opening or adjusted on-hand quantity; must be >= 0
 * @param reorderLevel  reorder trigger threshold; must be >= 0
 */
public record CreateProductRequest(

        @NotBlank(message = "Product name is required")
        String name,

        @NotBlank(message = "SKU is required")
        String sku,

        // nullable — not all products have barcodes; unique per tenant when set
        String barcode,

        @NotBlank(message = "Category ID is required")
        String categoryId,

        ProductUnit unit,

        @NotNull(message = "MRP is required")
        @Positive(message = "MRP must be greater than zero")
        BigDecimal mrp,

        @NotNull(message = "Selling price is required")
        @Positive(message = "Selling price must be greater than zero")
        BigDecimal sellingPrice,

        // nullable — not all tenants track cost price at product creation time
        BigDecimal costPrice,

        @NotNull(message = "GST rate is required")
        @DecimalMin(value = "0", message = "GST rate cannot be negative")
        BigDecimal gstRate,

        String hsnCode,

        @NotNull(message = "Current stock is required")
        @PositiveOrZero(message = "Current stock cannot be negative")
        BigDecimal currentStock,

        @NotNull(message = "Reorder level is required")
        @PositiveOrZero(message = "Reorder level cannot be negative")
        BigDecimal reorderLevel
) {
}
