package com.auradev.erp.inventory.entity;

import com.auradev.erp.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped live stock level — one row per product per tenant (schema v2.0).
 */
@Getter
@Setter
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "product_id"}))
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "low_stock_threshold", precision = 12, scale = 3)
    private BigDecimal lowStockThreshold;

    @Column(name = "reorder_quantity", precision = 12, scale = 3)
    private BigDecimal reorderQuantity;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated = Instant.now();
}
