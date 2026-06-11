package com.auradev.erp.purchase.entity;

import com.auradev.erp.common.entity.BaseEntity;
import com.auradev.erp.party.entity.Supplier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a purchase order / supplier bill.
 *
 * <p>A purchase begins as {@link PurchaseStatus#DRAFT}, advances to
 * {@link PurchaseStatus#PENDING_GRN} once a GRN is raised, then to
 * {@link PurchaseStatus#BILLED} once goods are received, and finally to
 * {@link PurchaseStatus#PAID} once the supplier is settled.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "purchases")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Purchase extends BaseEntity {

    /** Auto-generated human-readable purchase number, e.g. {@code ABC-2024-00001}. */
    @Column(name = "purchase_no", nullable = false, unique = true)
    private String purchaseNo;

    /** Supplier this purchase is raised against. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** Date shown on the supplier's bill / invoice. */
    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    /** Payment due date (may be {@code null} if terms are on delivery). */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Sum of all line-item amounts before tax. */
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    /** Total GST across all line items. */
    @Column(name = "gst_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal gstTotal;

    /** {@code subtotal + gstTotal} — the amount payable to the supplier. */
    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    /** Current lifecycle state of this purchase. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseStatus status;

    /** Free-text notes visible to staff. */
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Line items that make up this purchase. */
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();
}
