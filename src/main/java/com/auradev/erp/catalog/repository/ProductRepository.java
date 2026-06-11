package com.auradev.erp.catalog.repository;

import com.auradev.erp.catalog.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Data-access layer for {@link Product} entities.
 *
 * <p>All standard-derived queries assume the Hibernate {@code tenantFilter}
 * is enabled upstream.  Native/JPQL queries that bypass the filter include
 * an explicit {@code tenantId} predicate.</p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Paginated list of all products belonging to a tenant.
     * Rich filtering (name/sku/barcode/status) is handled in
     * {@link com.auradev.erp.catalog.service.CatalogService} via
     * Specification or in-memory post-filter on smaller catalogs.
     */
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Locate a product by its SKU within a specific tenant.
     * Used to enforce uniqueness and for barcode/keyboard lookup.
     */
    Optional<Product> findByTenantIdAndSku(UUID tenantId, String sku);

    /**
     * Locate a product by its barcode within a specific tenant.
     * Used for POS scan-to-add-to-cart flow.
     */
    Optional<Product> findByTenantIdAndBarcode(UUID tenantId, String barcode);

    /**
     * Return all products whose current stock is at or below their configured
     * reorder level — i.e. items needing replenishment.
     *
     * <p>OUT-of-stock (currentStock = 0) items are intentionally included so
     * that buyers can raise a purchase order for them.</p>
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.active = true
              AND p.currentStock <= p.reorderLevel
            ORDER BY p.currentStock ASC
            """)
    List<Product> findLowStockByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Pessimistic-write lock lookup used by
     * {@link com.auradev.erp.inventory.service.InventoryService} before any
     * stock mutation.  The lock prevents a concurrent transaction from reading
     * a stale stock value between our check and our update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Atomically decrement {@code currentStock} by {@code qty}, but only when
     * sufficient stock is available ({@code currentStock >= qty}).
     *
     * <p>Returns the number of rows affected:</p>
     * <ul>
     *   <li>{@code 1} — decrement succeeded; stock was adequate.</li>
     *   <li>{@code 0} — decrement rejected; stock was insufficient
     *       (oversell guard). The caller must decide whether to throw or allow
     *       negative stock based on tenant settings.</li>
     * </ul>
     *
     * <p>The {@code version} column is incremented here so that any concurrent
     * optimistic-lock holder reading the same product will detect the
     * conflict on their next flush.</p>
     */
    @Modifying
    @Query("""
            UPDATE Product p
            SET p.currentStock = p.currentStock - :qty,
                p.version      = p.version + 1
            WHERE p.id = :id
              AND p.currentStock >= :qty
            """)
    int decrementStock(@Param("id") UUID id, @Param("qty") BigDecimal qty);

    /**
     * Atomically increment {@code currentStock} by {@code qty}.
     * Used for GRN, returns, and positive manual adjustments.
     */
    @Modifying
    @Query("""
            UPDATE Product p
            SET p.currentStock = p.currentStock + :qty,
                p.version      = p.version + 1
            WHERE p.id = :id
            """)
    int incrementStock(@Param("id") UUID id, @Param("qty") BigDecimal qty);

    /**
     * Count products at or below their reorder level (including zero stock).
     * Used by the dashboard summary to populate the low-stock KPI.
     *
     * @param tenantId the tenant UUID
     * @return number of active products at or below reorder level
     */
    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.active = true
              AND p.currentStock <= p.reorderLevel
            """)
    long countLowStockByTenantId(@Param("tenantId") UUID tenantId);
}
