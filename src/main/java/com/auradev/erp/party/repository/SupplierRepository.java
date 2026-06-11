package com.auradev.erp.party.repository;

import com.auradev.erp.party.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Supplier} entities.
 */
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    /**
     * Return all suppliers for a tenant, paginated.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination and sort instructions
     * @return a page of suppliers
     */
    Page<Supplier> findByTenantId(UUID tenantId, Pageable pageable);
}
