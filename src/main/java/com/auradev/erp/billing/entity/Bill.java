package com.auradev.erp.billing.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.auradev.erp.settings.model.GstScheme;

@Getter
@Setter
@Entity
@Table(name = "bills")
public class Bill extends BaseEntity {

    @Column(name = "bill_no", nullable = false)
    private String billNo;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "place_of_supply_state", length = 2)
    private String placeOfSupplyState;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "bill_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal billDiscount;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_mode", nullable = false, columnDefinition = "discount_mode")
    private DiscountMode discountMode = DiscountMode.AMOUNT;

    @Column(name = "cgst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal cgstTotal;

    @Column(name = "sgst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal sgstTotal;

    @Column(name = "igst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal igstTotal;

    @Column(name = "round_off", nullable = false, precision = 12, scale = 2)
    private BigDecimal roundOff;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, columnDefinition = "bill_payment_status")
    private BillPaymentStatus paymentStatus;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "bill_status")
    private BillStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "receipt_url")
    private String receiptUrl;

    /** GST scheme in effect when the bill was finalised (snapshot for audit / reprint). */
    @Column(name = "gst_scheme", nullable = false, length = 20)
    private String gstScheme = GstScheme.PRODUCT.name();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();
}
