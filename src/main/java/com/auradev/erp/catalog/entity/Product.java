package com.auradev.erp.catalog.entity;

import com.auradev.erp.common.entity.CatalogEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Shared SKU catalogue — schema v2.0 (no tenant_id, no stock columns).
 * Stock lives in {@link com.auradev.erp.inventory.entity.Inventory}.
 */
@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends CatalogEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "barcode", unique = true, length = 50)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, columnDefinition = "unit_type")
    private UnitType unitType = UnitType.unit;

    @Column(name = "unit_label", nullable = false, length = 20)
    private String unitLabel = "pcs";

    @Column(name = "price_mrp", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMrp;

    @Column(name = "price_selling", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSelling;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "tax_rate_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRatePct;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Transient
    public StockStatus stockStatus(BigDecimal quantityOnHand, BigDecimal lowStockThreshold) {
        if (quantityOnHand == null || quantityOnHand.compareTo(BigDecimal.ZERO) <= 0) {
            return StockStatus.OUT;
        }
        if (lowStockThreshold != null && quantityOnHand.compareTo(lowStockThreshold) <= 0) {
            return StockStatus.LOW;
        }
        return StockStatus.IN;
    }
}
