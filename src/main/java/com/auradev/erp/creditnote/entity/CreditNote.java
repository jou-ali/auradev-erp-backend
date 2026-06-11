package com.auradev.erp.creditnote.entity;

import com.auradev.erp.billing.entity.Bill;
import com.auradev.erp.common.entity.BaseEntity;
import com.auradev.erp.party.entity.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A credit note issued against an original sales bill.
 *
 * <p>Credit notes are immutable after creation — they can only be created, not
 * edited.  The {@code originalBill} link traces the return back to the sale.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "credit_notes")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CreditNote extends BaseEntity {

    @Column(name = "credit_note_no", nullable = false)
    private String creditNoteNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_bill_id", nullable = false, updatable = false)
    private Bill originalBill;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "cgst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal cgstTotal = BigDecimal.ZERO;

    @Column(name = "sgst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal sgstTotal = BigDecimal.ZERO;

    @Column(name = "igst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal igstTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CreditNoteItem> items = new ArrayList<>();
}
