package com.auradev.erp.audit.repository;

import com.auradev.erp.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data-access layer for {@link AuditLog} entries.
 *
 * <p>Audit log entries are append-only.  This repository provides no
 * update or delete methods beyond the inherited base class.</p>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Return all audit log entries for a tenant, newest first.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination and sort instructions
     * @return a page of audit log entries ordered by {@code createdAt DESC}
     */
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
