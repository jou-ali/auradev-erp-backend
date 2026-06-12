package com.auradev.erp.billing.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "customer_type")
    private CustomerType type = CustomerType.walkin;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints;

    @Column(name = "credit_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;
}
