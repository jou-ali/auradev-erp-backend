package com.auradev.erp.tenant.repository;

import com.auradev.erp.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data-access layer for {@link Tenant} entities.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
