package com.auradev.erp.billing.entity;

import com.auradev.erp.common.entity.BaseEntity;
import com.auradev.erp.party.entity.Customer;
import com.auradev.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A sales bill / tax invoice issued to a customer.
 *
 * <p>Tenant isolation is enforced via the Hibernate {@code tenantFilter}. All
 * monetary columns use {@code NUMERIC(12,2)} to avoid floating-point drift.</p>
 *
 * <p>{@code customer} and {@code cashier} are lazy {@code @ManyToOne} associations.
 * They are loaded on demand; the service layer must initialise them within an
 * open session.  The corresponding foreign-key columns ({@code customer_id},
 * {@code cashier_id}) are mapped via {@code @JoinColumn}.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "bills")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Bill extends BaseEntity {

    @Column(name = "bill_no", nullable = false)
    private String billNo;

    /**
     * Customer to whom this bill is issued.
     * Nullable for walk-in sales where no customer account is linked.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * The cashier / staff member who raised this bill.
     * Set from the authenticated principal at creation time.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    /** Place of supply state code (2-char ISO) used for intra/inter-state GST determination. */
    @Column(name = "place_of_supply_state", length = 2)
    private String placeOfSupplyState;

    /** Sum of all line taxable values before bill-level discount. */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "bill_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal billDiscount = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_mode", nullable = false)
    private DiscountMode discountMode = DiscountMode.AMOUNT;

    @Column(name = "cgst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal cgstTotal = BigDecimal.ZERO;

    @Column(name = "sgst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal sgstTotal = BigDecimal.ZERO;

    @Column(name = "igst_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal igstTotal = BigDecimal.ZERO;

    @Column(name = "round_off", nullable = false, precision = 12, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillStatus status = BillStatus.COMPLETED;

    /** Client-supplied deduplication key (stored hashed if needed; plain here for simplicity). */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    /** URL to the generated PDF receipt in Supabase Storage / S3. */
    @Column(name = "receipt_url")
    private String receiptUrl;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();
}
