package com.auradev.erp.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract JPA base entity providing audit fields, tenant isolation, and
 * optimistic-locking support for all concrete entities in the application.
 *
 * <p>Tenant isolation ({@code @FilterDef} / {@code @Filter}) is intentionally
 * omitted here and must be declared on each concrete entity, because Hibernate
 * requires the filter annotations to reference a mapped table.</p>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tenant the record belongs to.  Set once at creation and never changed.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** UUID of the user who created this record (populated by the application layer). */
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    /** UUID of the user who last modified this record. */
    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Optimistic-locking version counter managed by JPA.
     * A {@code null} value is valid for detached/new entities before the first
     * flush; Hibernate will initialise it to {@code 0} on first insert.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
