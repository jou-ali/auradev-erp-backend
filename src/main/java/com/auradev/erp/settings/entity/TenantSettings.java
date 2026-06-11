package com.auradev.erp.settings.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-tenant configuration stored as JSONB columns.
 *
 * <p>The {@code tax} JSONB field is projected into {@link #priceIncludesTax}
 * via a formula column. For simplicity, only the fields consumed by
 * {@code BillingService} are mapped here; the full JSONB payloads remain
 * available via {@link #taxJson} for read-all use cases.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "tenant_settings")
public class TenantSettings {

    @Id
    @Column(name = "tenant_id", updatable = false, nullable = false)
    private UUID tenantId;

    /**
     * Whether selling prices already include GST (MRP billing).
     * Derived from the {@code tax} JSONB field using a Hibernate formula.
     *
     * <p>The formula reads the {@code priceIncludesTax} key out of the JSONB column
     * at query time so that the service layer can consume a plain boolean without
     * deserialising the full JSON blob.  {@code null} is treated as {@code false}
     * (tax-exclusive pricing) by {@link #isPriceIncludesTax()}.</p>
     */
    @Formula("(tax->>'priceIncludesTax')::boolean")
    private Boolean priceIncludesTax;

    /**
     * Raw tax JSONB payload — kept for settings read/write endpoints.
     */
    @Column(name = "tax", columnDefinition = "jsonb")
    private String taxJson;

    @Column(name = "payments", columnDefinition = "jsonb")
    private String paymentsJson;

    @Column(name = "printer", columnDefinition = "jsonb")
    private String printerJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if prices are tax-inclusive; defaults to {@code false}
     * when the setting has not been explicitly configured.
     */
    public boolean isPriceIncludesTax() {
        return Boolean.TRUE.equals(priceIncludesTax);
    }
}
