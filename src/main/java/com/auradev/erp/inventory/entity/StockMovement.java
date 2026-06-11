package com.auradev.erp.inventory.entity;

import com.auradev.erp.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ledger entry recording a single stock-level change for a product.
 *
 * <p><strong>Append-only design:</strong> movements are never updated or
 * deleted.  Corrections are made by posting a new movement with the inverse
 * delta and {@link MovementReason#COUNT_CORRECTION}.  For this reason the
 * entity intentionally omits {@code @Version} — optimistic locking is
 * irrelevant for an insert-only table.</p>
 *
 * <p>The {@code balanceAfter} column captures the product's
 * {@code currentStock} immediately after this movement was applied,
 * providing a running-balance ledger without requiring a full replay of all
 * movements to reconstruct stock at any point in time.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "stock_movements")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant this movement belongs to — denormalised for query performance. */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * The product whose stock changed.
     * Eager fetch is acceptable here because every movement response
     * needs the product name and SKU.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    /**
     * Signed quantity change.
     * Positive values increase stock (GRN, return); negative values decrease
     * it (sale, damage).
     */
    @Column(name = "delta", nullable = false, precision = 12, scale = 4)
    private BigDecimal delta;

    /** Business reason for the movement. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private MovementReason reason;

    /** Type of the originating business document; nullable for ad-hoc adjustments. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 32)
    private MovementRefType referenceType;

    /** UUID of the originating document; nullable when referenceType is null. */
    @Column(name = "reference_id")
    private UUID referenceId;

    /**
     * Running balance captured after this movement was applied.
     * Allows instant balance reconstruction without replaying all movements.
     */
    @Column(name = "balance_after", nullable = false, precision = 12, scale = 4)
    private BigDecimal balanceAfter;

    /** Optional free-text note explaining the movement. */
    @Column(name = "notes", length = 512)
    private String notes;

    /** UUID of the user who initiated this movement. */
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    /**
     * Wall-clock time at which this movement was recorded.
     * Set explicitly by the service layer (not via Spring auditing) so that
     * the value is correct even in bulk-import scenarios.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
