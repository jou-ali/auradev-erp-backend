package com.auradev.erp.procurement.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "purchases")
public class Purchase extends BaseEntity {

    @Column(name = "purchase_no", nullable = false)
    private String purchaseNo;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "gst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal gstTotal;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "purchase_status")
    private PurchaseStatus status;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();
}
