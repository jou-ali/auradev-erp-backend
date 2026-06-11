package com.auradev.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant represents a single merchant or store installation in the ERP.
 *
 * <p>Each tenant has its own isolated data partition (shared-schema multi-tenancy
 * enforced via Hibernate filters).  Tenant-level settings such as state code,
 * bill prefix, and tax configuration are stored here and in {@code tenant_settings}.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "tenants")
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    /** GST Identification Number of the business. */
    @Column(name = "gstin")
    private String gstin;

    /**
     * 2-character state code used to determine intra/inter-state GST billing.
     * Defaults to 29 (Karnataka).
     */
    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode = "29";

    @Column(name = "address")
    private String address;

    /** Prefix prepended to the bill number sequence (e.g. "ERP", "INV"). */
    @Column(name = "bill_no_prefix", nullable = false)
    private String billNoPrefix = "ERP";

    @Column(name = "bill_footer")
    private String billFooter;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
