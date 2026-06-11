package com.auradev.erp.inventory.repository;

import com.auradev.erp.analytics.projection.ActivityProjection;
import com.auradev.erp.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data-access layer for {@link StockMovement} ledger entries.
 *
 * <p>This table is append-only; the repository therefore provides no
 * save-or-update derived methods beyond the inherited {@code save}.</p>
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    /**
     * Paginated movement history for a single product, newest first.
     * Used on the product detail screen to show a running ledger.
     *
     * @param productId UUID of the product
     * @param pageable  page / sort parameters
     * @return page of movements
     */
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    /**
     * The 50 most recent movements across the entire tenant, newest first.
     * Used to populate the inventory activity feed on the dashboard.
     *
     * @param tenantId the tenant whose movements to retrieve
     * @return up to 50 movements
     */
    List<StockMovement> findTop50ByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * Check whether any movement references the given product.
     * Used by the catalog service to guard against deleting products that
     * have a movement history (would orphan ledger rows).
     *
     * @param productId UUID of the product to check
     * @return {@code true} if at least one movement exists for this product
     */
    @Query("SELECT COUNT(m) > 0 FROM StockMovement m WHERE m.product.id = :productId")
    boolean existsByProductId(@Param("productId") UUID productId);

    /**
     * Recent stock movement activity items for the activity feed.
     * Returns the most recent movements for a tenant, formatted for display.
     *
     * @param tenantId the tenant UUID
     * @param limit    maximum number of items to return
     * @return list of activity items
     */
    @Query(value = """
            SELECT COALESCE(u.name, 'System')    AS who,
                   LOWER(sm.reason)               AS action,
                   p.name                         AS detail,
                   sm.created_at                  AS time,
                   'StockMovement'                AS entityType
            FROM stock_movements sm
            JOIN products p ON p.id = sm.product_id
            LEFT JOIN users u ON u.id = sm.created_by
            WHERE sm.tenant_id = :tenantId
            ORDER BY sm.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ActivityProjection> recentMovementActivity(@Param("tenantId") UUID tenantId, @Param("limit") int limit);
}
