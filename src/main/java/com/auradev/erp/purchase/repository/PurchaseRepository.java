package com.auradev.erp.purchase.repository;

import com.auradev.erp.purchase.entity.Purchase;
import com.auradev.erp.purchase.entity.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Purchase} entities.
 *
 * <p>All query methods are scoped by {@code tenantId}; the Hibernate
 * {@code tenantFilter} must also be enabled at the session level for
 * {@code findAll} and other non-parameterised queries.</p>
 */
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    /**
     * Return all purchases for a tenant, most-recent first, paginated.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination and sort instructions
     * @return a page of purchases
     */
    Page<Purchase> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Return purchases for a tenant filtered by lifecycle status.
     *
     * @param tenantId the tenant UUID
     * @param status   the desired status
     * @param pageable pagination and sort instructions
     * @return a page of matching purchases
     */
    Page<Purchase> findByTenantIdAndStatus(UUID tenantId, PurchaseStatus status, Pageable pageable);

    /**
     * Look up a single purchase by its human-readable number within a tenant.
     *
     * @param tenantId   the tenant UUID
     * @param purchaseNo the generated purchase number, e.g. {@code ABC-2024-00001}
     * @return the matching purchase, if any
     */
    Optional<Purchase> findByTenantIdAndPurchaseNo(UUID tenantId, String purchaseNo);
}
