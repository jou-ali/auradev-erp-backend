package com.auradev.erp.inventory.service;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.inventory.dto.StockAdjustRequest;
import com.auradev.erp.inventory.dto.StockMovementResponse;
import com.auradev.erp.inventory.entity.Inventory;
import com.auradev.erp.inventory.entity.MovementType;
import com.auradev.erp.inventory.entity.RefType;
import com.auradev.erp.inventory.entity.StockMovement;
import com.auradev.erp.inventory.repository.InventoryRepository;
import com.auradev.erp.inventory.repository.StockMovementRepository;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository movementRepository;

    public StockMovement recordMovement(
            UUID productId,
            MovementType movementType,
            BigDecimal quantity,
            RefType refType,
            UUID refId,
            String notes) {

        UUID tenantId = TenantContext.require();
        UUID userId = requireCurrentUserId();

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "Quantity must be positive");
        }

        productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        Inventory inv = inventoryRepository.findForUpdate(tenantId, productId)
                .orElseGet(() -> createInventoryRow(tenantId, productId));

        if (isStockOut(movementType)) {
            int updated = inventoryRepository.decrement(tenantId, productId, quantity);
            if (updated == 0) {
                throw new BusinessException(
                        "INSUFFICIENT_STOCK",
                        "Insufficient stock for product " + productId);
            }
        } else {
            inventoryRepository.increment(tenantId, productId, quantity);
        }

        Inventory refreshed = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory", productId));

        StockMovement movement = new StockMovement();
        movement.setTenantId(tenantId);
        movement.setProduct(refreshed.getProduct());
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setQuantityAfter(refreshed.getQuantityOnHand());
        movement.setReferenceType(refType != null ? refType : RefType.manual);
        movement.setReferenceId(refId);
        movement.setNotes(notes);
        movement.setCreatedBy(userId);
        movement.setCreatedAt(Instant.now());

        StockMovement saved = movementRepository.save(movement);
        log.info("Movement recorded product={} type={} qty={} balance={}",
                productId, movementType, quantity, refreshed.getQuantityOnHand());
        return saved;
    }

    public void ensureInventoryRow(UUID tenantId, UUID productId, BigDecimal initialStock,
                                   BigDecimal lowStockThreshold, BigDecimal reorderQuantity) {
        if (inventoryRepository.findByTenantIdAndProductId(tenantId, productId).isPresent()) {
            return;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        Inventory inv = new Inventory();
        inv.setTenantId(tenantId);
        inv.setProduct(product);
        inv.setQuantityOnHand(initialStock != null ? initialStock : BigDecimal.ZERO);
        inv.setLowStockThreshold(lowStockThreshold);
        inv.setReorderQuantity(reorderQuantity);
        inv.setLastUpdated(Instant.now());
        inventoryRepository.save(inv);
    }

    public void updateThresholds(UUID tenantId, UUID productId,
                                 BigDecimal lowStockThreshold, BigDecimal reorderQuantity) {
        Inventory inv = inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory", productId));
        inv.setLowStockThreshold(lowStockThreshold);
        inv.setReorderQuantity(reorderQuantity);
        inv.setLastUpdated(Instant.now());
        inventoryRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public Inventory getInventory(UUID tenantId, UUID productId) {
        return inventoryRepository.findByTenantIdAndProductId(tenantId, productId).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getMovements(UUID productId, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        Page<StockMovement> page = movementRepository
                .findByTenantIdAndProductIdOrderByCreatedAtDesc(tenantId, productId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public boolean hasMovements(UUID productId) {
        return movementRepository.existsByProductId(productId);
    }

    public void bulkAdjust(List<StockAdjustRequest> adjustments) {
        for (StockAdjustRequest req : adjustments) {
            recordMovement(req.productId(), req.movementType(), req.quantity(),
                    RefType.manual, null, req.notes());
        }
    }

    private Inventory createInventoryRow(UUID tenantId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
        Inventory inv = new Inventory();
        inv.setTenantId(tenantId);
        inv.setProduct(product);
        inv.setQuantityOnHand(BigDecimal.ZERO);
        inv.setLastUpdated(Instant.now());
        return inventoryRepository.save(inv);
    }

    private boolean isStockOut(MovementType type) {
        return type == MovementType.sale
                || type == MovementType.adjustment_out
                || type == MovementType.waste;
    }

    private StockMovementResponse toResponse(StockMovement m) {
        BigDecimal signed = isStockOut(m.getMovementType())
                ? m.getQuantity().negate()
                : m.getQuantity();
        return new StockMovementResponse(
                m.getId(),
                m.getProduct().getName(),
                m.getProduct().getSku(),
                m.getMovementType(),
                m.getQuantity(),
                signed,
                m.getReferenceId(),
                m.getQuantityAfter(),
                m.getNotes(),
                m.getCreatedAt()
        );
    }

    private UUID requireCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up.getId();
        }
        throw new BusinessException("UNAUTHENTICATED", "User context required for stock movements");
    }
}
