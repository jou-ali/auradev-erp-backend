package com.auradev.erp.inventory.service;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.inventory.dto.StockAdjustRequest;
import com.auradev.erp.inventory.dto.StockMovementResponse;
import com.auradev.erp.inventory.entity.MovementReason;
import com.auradev.erp.inventory.entity.MovementRefType;
import com.auradev.erp.inventory.entity.StockMovement;
import com.auradev.erp.inventory.repository.StockMovementRepository;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core inventory service — the single authoritative point for all stock mutations.
 *
 * <p><strong>Concurrency model</strong></p>
 * <ol>
 *   <li>Before any mutation the product row is acquired with a
 *       {@code SELECT … FOR UPDATE} (pessimistic write lock) to prevent
 *       concurrent sessions from reading a stale balance.</li>
 *   <li>Decrements use an atomic JPQL update with a {@code currentStock >= qty}
 *       guard.  If the query returns 0 rows the decrement was rejected due to
 *       insufficient stock, and the service checks tenant settings before
 *       deciding to throw or allow negative stock.</li>
 *   <li>Increments also use an atomic JPQL update so that the version counter
 *       is bumped and any concurrent optimistic-lock holder detects the
 *       conflict.</li>
 *   <li>After either update the product is re-fetched to obtain the accurate
 *       {@code balanceAfter} value recorded on the movement ledger row.</li>
 * </ol>
 *
 * <p><strong>Negative stock:</strong> By default, negative stock is rejected.
 * If the tenant ever enables an "allow negative stock" flag in settings
 * (not yet modelled), the check in {@link #recordMovement} must consult that
 * flag before throwing {@code INSUFFICIENT_STOCK}.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository       productRepository;
    private final StockMovementRepository movementRepository;

    // -------------------------------------------------------------------------
    // Core mutation
    // -------------------------------------------------------------------------

    /**
     * Record a single stock movement and update the product's running balance.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Load product with {@code SELECT FOR UPDATE} (pessimistic write lock).</li>
     *   <li>If {@code delta < 0}: run atomic decrement guard; if 0 rows affected
     *       and tenant does not allow negative stock → throw
     *       {@code INSUFFICIENT_STOCK}.</li>
     *   <li>If {@code delta > 0}: atomic increment query.</li>
     *   <li>Re-load product to capture authoritative {@code balanceAfter}.</li>
     *   <li>Insert {@link StockMovement} ledger row.</li>
     * </ol>
     *
     * @param productId   UUID of the product to adjust
     * @param delta       signed quantity change (negative = stock out)
     * @param reason      business reason for the movement
     * @param refType     type of originating document; {@code null} for ad-hoc
     * @param refId       UUID of the originating document; {@code null} for ad-hoc
     * @param notes       optional free-text note
     * @return the persisted {@link StockMovement}
     * @throws EntityNotFoundException if the product does not exist in the current tenant
     * @throws BusinessException       with code {@code INSUFFICIENT_STOCK} if a negative
     *                                 delta would drive stock below zero
     */
    public StockMovement recordMovement(
            UUID productId,
            BigDecimal delta,
            MovementReason reason,
            MovementRefType refType,
            UUID refId,
            String notes) {

        UUID tenantId = TenantContext.require();

        // Step 1 — acquire pessimistic write lock to serialise concurrent mutations.
        Product product = productRepository.findByIdForUpdate(productId)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        // Steps 2 / 3 — atomic stock update.
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal qty = delta.abs();
            int updated = productRepository.decrementStock(productId, qty);
            if (updated == 0) {
                // Decrement guard triggered: insufficient stock.
                // NOTE: if a future TenantSettings entity introduces an
                // "allowNegativeStock" flag, read it here before throwing.
                throw new BusinessException(
                        "INSUFFICIENT_STOCK",
                        "Insufficient stock for product '" + product.getName()
                        + "' (SKU: " + product.getSku() + "): requested "
                        + qty + ", available " + product.getCurrentStock());
            }
        } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
            productRepository.incrementStock(productId, delta);
        }
        // delta == 0 is a no-op on the product balance but still records a
        // movement row (useful as an audit note or COUNT_CORRECTION confirmation).

        // Step 4 — re-load the updated product to capture the authoritative balance.
        Product refreshed = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        // Step 5 — insert the ledger row.
        StockMovement movement = new StockMovement();
        movement.setTenantId(tenantId);
        movement.setProduct(refreshed);
        movement.setDelta(delta);
        movement.setReason(reason);
        movement.setReferenceType(refType);
        movement.setReferenceId(refId);
        movement.setBalanceAfter(refreshed.getCurrentStock());
        movement.setNotes(notes);
        movement.setCreatedBy(currentUserId());
        movement.setCreatedAt(Instant.now());

        StockMovement saved = movementRepository.save(movement);

        log.info("Stock movement recorded: product={} delta={} reason={} balance={}",
                productId, delta, reason, refreshed.getCurrentStock());

        return saved;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Paginated movement history for a single product, newest first.
     *
     * @param productId UUID of the product
     * @param pageable  page / sort parameters
     * @return paginated response DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getMovements(UUID productId, Pageable pageable) {
        Page<StockMovement> page =
                movementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    /**
     * Check whether any stock movement references the given product.
     * Called by the catalog service to guard soft-deletes.
     *
     * @param productId UUID of the product
     * @return {@code true} if at least one movement exists
     */
    @Transactional(readOnly = true)
    public boolean hasMovements(UUID productId) {
        return movementRepository.existsByProductId(productId);
    }

    // -------------------------------------------------------------------------
    // Bulk
    // -------------------------------------------------------------------------

    /**
     * Apply a list of stock adjustments within a single transaction.
     *
     * <p>If any individual adjustment fails (e.g. insufficient stock), the
     * entire batch is rolled back.  Callers that need partial-success
     * semantics should invoke {@link #recordMovement} per item with separate
     * transaction boundaries.</p>
     *
     * @param adjustments list of adjustments to apply; must not be {@code null}
     */
    public void bulkAdjust(List<StockAdjustRequest> adjustments) {
        for (StockAdjustRequest req : adjustments) {
            recordMovement(
                    req.productId(),
                    req.delta(),
                    req.reason(),
                    MovementRefType.ADJUSTMENT,
                    null,
                    req.notes()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private StockMovementResponse toResponse(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getProduct().getName(),
                m.getProduct().getSku(),
                m.getDelta(),
                m.getReason(),
                m.getReferenceId(),
                m.getBalanceAfter(),
                m.getNotes(),
                m.getCreatedAt()
        );
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
