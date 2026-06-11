package com.auradev.erp.party.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A customer of the tenant — walk-in, retail, or business.
 *
 * <p>Loyalty points and credit balance are maintained in this record and
 * updated by the billing and returns services.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "customers")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Customer extends BaseEntity {

    /** Full display name of the customer. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Primary contact phone number. */
    @Column(name = "phone")
    private String phone;

    /** Contact email address. */
    @Column(name = "email")
    private String email;

    /** Customer classification. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CustomerType type;

    /** GST Identification Number — applicable for B2B customers. */
    @Column(name = "gstin", length = 15)
    private String gstin;

    /** 2-digit state code used for inter-state GST determination. */
    @Column(name = "state_code", length = 2)
    private String stateCode;

    /** Postal / delivery address. */
    @Column(name = "address", columnDefinition = "text")
    private String address;

    /** Accumulated loyalty points; starts at 0. */
    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints;

    /**
     * Outstanding credit balance available to the customer
     * (e.g. from credit notes or advance payments).
     */
    @Column(name = "credit_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditBalance;
}
