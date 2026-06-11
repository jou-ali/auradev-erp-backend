package com.auradev.erp.audit.entity;

import com.auradev.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable record of a business event performed by a user within a tenant.
 *
 * <p>Audit log entries are written asynchronously and must never be updated
 * or deleted.  The {@code metadata} JSON column carries event-specific context
 * (e.g. before/after values, affected field names).</p>
 */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Tenant in whose context the action occurred; null for SUPER_ADMIN cross-tenant actions. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /**
     * User who triggered the action.  Loaded lazily — avoid eager fetching audit logs
     * as the user table may be large.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Short machine-readable label, e.g. {@code LOGIN_SUCCESS}, {@code BILL_CREATED}. */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    /** The class name of the affected entity, e.g. {@code Bill}, {@code Product}. */
    @Column(name = "entity_type", length = 64)
    private String entityType;

    /** UUID of the affected entity record; null for non-entity-specific events. */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Arbitrary event-specific metadata (e.g. changed field values).
     * Stored as a JSONB column for efficient querying.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /** Originating client IP address; null if unavailable (e.g. internal system events). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** UTC timestamp at which the event was recorded. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
