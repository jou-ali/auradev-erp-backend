package com.auradev.erp.catalog.dto;

import com.auradev.erp.catalog.entity.ProductUnit;
import com.auradev.erp.catalog.entity.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.auradev.erp.catalog.entity.Product}.
 *
 * <p>{@code costPrice} is nullable in this record.  The service layer sets it
 * to {@code null} when the caller's role does not grant access to cost
 * information (e.g. {@link com.auradev.erp.user.entity.UserRole#CASHIER}).</p>
 *
 * @param id            product UUID
 * @param name          display name
 * @param sku           stock-keeping unit code
 * @param barcode       barcode value; may be {@code null}
 * @param categoryName  parent category name; may be {@code null}
 * @param unit          unit of measure
 * @param mrp           maximum retail price
 * @param sellingPrice  price charged to customers
 * @param costPrice     landed/purchase cost — {@code null} for restricted roles
 * @param gstRate       GST percentage (e.g. {@code 18.00})
 * @param hsnCode       HSN/SAC code for GST compliance
 * @param currentStock  on-hand quantity
 * @param reorderLevel  quantity at which replenishment is triggered
 * @param stockStatus   derived IN / LOW / OUT indicator
 * @param active        whether the product is listed for sale
 * @param createdAt     creation timestamp (UTC)
 */
public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String barcode,
        String categoryName,
        ProductUnit unit,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        BigDecimal costPrice,        // null for CASHIER role
        BigDecimal gstRate,
        String hsnCode,
        BigDecimal currentStock,
        BigDecimal reorderLevel,
        StockStatus stockStatus,
        boolean active,
        Instant createdAt
) {
}
