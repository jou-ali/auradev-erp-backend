package com.auradev.erp.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single tender row associated with a {@link Bill}.
 *
 * <p>Split payments are represented as multiple rows, each with
 * {@link PaymentMethod#SPLIT_COMPONENT} and the sub-amount for that tender type.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Amount tendered by the customer (relevant for CASH payments). */
    @Column(name = "tendered", precision = 12, scale = 2)
    private BigDecimal tendered;

    /** Change to be returned to the customer. */
    @Column(name = "change_due", precision = 12, scale = 2)
    private BigDecimal changeDue;

    /** UPI reference ID, card approval code, etc. */
    @Column(name = "reference")
    private String reference;
}
