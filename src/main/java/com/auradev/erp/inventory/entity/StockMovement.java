package com.auradev.erp.inventory.entity;

import com.auradev.erp.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable stock movement audit trail — schema v2.0.
 */
@Getter
@Setter
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(name = "reference_id")
    private UUID referenceId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", columnDefinition = "ref_type")
    private RefType referenceType;

    @Convert(converter = MovementTypeConverter.class)
    @Column(name = "movement_type", nullable = false, columnDefinition = "movement_type")
    private MovementType movementType;

    /** Always positive — direction implied by movement_type. */
    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "quantity_after", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityAfter;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
