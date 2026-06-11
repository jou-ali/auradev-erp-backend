package com.auradev.erp.catalog.repository;

import com.auradev.erp.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link Category} entities.
 *
 * <p>All queries assume the Hibernate {@code tenantFilter} has been enabled
 * upstream by the service layer.</p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Return all categories for a tenant, ordered by display position then name.
     */
    List<Category> findByTenantIdOrderBySortOrderAscNameAsc(UUID tenantId);

    /**
     * Look up a category by its exact name within a tenant.
     * Used to prevent duplicate category names per tenant.
     */
    Optional<Category> findByTenantIdAndName(UUID tenantId, String name);
}
