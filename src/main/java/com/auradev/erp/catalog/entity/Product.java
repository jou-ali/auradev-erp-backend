package com.auradev.erp.catalog.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A sellable product in the tenant's catalog.
 *
 * <p>Stock management is handled cooperatively between this entity and
 * {@link com.auradev.erp.inventory.entity.StockMovement}.  Atomic stock
 * decrements are performed via a dedicated JPQL update query in
 * {@code ProductRepository} to prevent overselling under concurrent POS
 * sessions.</p>
 *
 * <p>The {@link #stockStatus()} transient method derives a
 * {@link StockStatus} value from {@code currentStock} vs
 * {@code reorderLevel} and is never persisted.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "products")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Product extends BaseEntity {

    /** Category this product belongs to; may be null for uncategorised products. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Human-readable product name shown on receipts and the POS screen. */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Stock-keeping unit — unique per tenant.
     * Used for inventory reconciliation and search.
     */
    @Column(name = "sku", nullable = false)
    private String sku;

    /**
     * Barcode value (EAN-13, QR, etc.).  Nullable; unique per tenant when set.
     * Used for fast POS scan lookup.
     */
    @Column(name = "barcode")
    private String barcode;

    /** Unit of measure (pieces, kilograms, etc.). */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private ProductUnit unit;

    /** Maximum retail price printed on packaging. */
    @Column(name = "mrp", nullable = false, precision = 12, scale = 4)
    private BigDecimal mrp;

    /** The price at which the product is sold to the customer. */
    @Column(name = "selling_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal sellingPrice;

    /**
     * Landed cost of the product.
     * Sensitive — stripped from API responses for CASHIER-role callers.
     */
    @Column(name = "cost_price", precision = 12, scale = 4)
    private BigDecimal costPrice;

    /**
     * GST rate as a percentage (e.g. {@code 18.00} for 18 %).
     * Used by the billing engine to compute CGST/SGST/IGST split.
     */
    @Column(name = "gst_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal gstRate;

    /** HSN / SAC code required for GST compliance on the invoice. */
    @Column(name = "hsn_code")
    private String hsnCode;

    /**
     * Running stock balance.  Updated atomically via
     * {@code ProductRepository#decrementStock} and the positive-delta JPQL
     * update in {@link com.auradev.erp.inventory.service.InventoryService}.
     */
    @Column(name = "current_stock", nullable = false, precision = 12, scale = 4)
    private BigDecimal currentStock = BigDecimal.ZERO;

    /**
     * Threshold at or below which a reorder alert should be raised.
     * Used by {@link #stockStatus()} and low-stock reporting queries.
     */
    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 4)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    /** Whether the product is listed / sellable. Soft-delete via active=false. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // -------------------------------------------------------------------------
    // Derived helper
    // -------------------------------------------------------------------------

    /**
     * Derive the stock status from the current on-hand quantity relative to
     * the reorder level.  This value is computed in-memory and is never
     * persisted.
     *
     * @return {@link StockStatus#OUT}  if {@code currentStock <= 0}
     *         {@link StockStatus#LOW}  if {@code 0 < currentStock <= reorderLevel}
     *         {@link StockStatus#IN}   otherwise
     */
    @Transient
    public StockStatus stockStatus() {
        if (currentStock == null || currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return StockStatus.OUT;
        }
        if (reorderLevel != null && currentStock.compareTo(reorderLevel) <= 0) {
            return StockStatus.LOW;
        }
        return StockStatus.IN;
    }
}
