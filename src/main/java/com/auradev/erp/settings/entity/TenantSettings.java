package com.auradev.erp.settings.entity;

import com.auradev.erp.settings.model.BillingConfig;
import com.auradev.erp.settings.model.PrinterConfig;
import com.auradev.erp.settings.model.TaxConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tenant_settings")
public class TenantSettings {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax", nullable = false)
    private TaxConfig tax = TaxConfig.defaults();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payments", nullable = false)
    private Map<String, Object> payments = Map.of(
            "cash", true,
            "upi", true,
            "card", false,
            "credit", true);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "printer", nullable = false)
    private PrinterConfig printer = PrinterConfig.defaults();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing", nullable = false)
    private BillingConfig billing = BillingConfig.defaults();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = Instant.now();
    }
}
