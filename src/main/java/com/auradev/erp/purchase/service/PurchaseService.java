package com.auradev.erp.purchase.service;

import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.common.util.MoneyUtils;
import com.auradev.erp.inventory.entity.MovementReason;
import com.auradev.erp.inventory.entity.MovementRefType;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.party.entity.Supplier;
import com.auradev.erp.party.repository.SupplierRepository;
import com.auradev.erp.purchase.dto.CreatePurchaseRequest;
import com.auradev.erp.purchase.dto.CreatePurchaseRequest.PurchaseItemRequest;
import com.auradev.erp.purchase.dto.PurchaseResponse;
import com.auradev.erp.purchase.dto.PurchaseResponse.PurchaseItemResponse;
import com.auradev.erp.purchase.entity.Purchase;
import com.auradev.erp.purchase.entity.PurchaseItem;
import com.auradev.erp.purchase.entity.PurchaseStatus;
import com.auradev.erp.purchase.repository.PurchaseRepository;
import com.auradev.erp.sequence.SequenceService;
import com.auradev.erp.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Domain service for managing purchases (supplier bills and GRN workflow).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final SequenceService sequenceService;
    private final AuditService auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Create a new purchase in {@link PurchaseStatus#DRAFT} state.
     *
     * <p>A unique purchase number is generated via {@link SequenceService},
     * line-item amounts are calculated, and totals are derived before
     * persisting.</p>
     *
     * @param req the incoming request
     * @return the saved purchase as a response DTO
     */
    public PurchaseResponse create(CreatePurchaseRequest req) {
        UUID tenantId = TenantContext.require();

        Supplier supplier = supplierRepository.findById(req.supplierId())
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + req.supplierId()));

        String purchaseNo = sequenceService.nextPurchaseNo(tenantId);

        Purchase purchase = new Purchase();
        purchase.setTenantId(tenantId);
        purchase.setPurchaseNo(purchaseNo);
        purchase.setSupplier(supplier);
        purchase.setBillDate(req.billDate());
        purchase.setDueDate(req.dueDate());
        purchase.setNotes(req.notes());
        purchase.setStatus(PurchaseStatus.DRAFT);

        List<PurchaseItem> items = buildItems(purchase, req.items(), tenantId);
        purchase.setItems(items);

        BigDecimal subtotal = items.stream()
                .map(PurchaseItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstTotal = items.stream()
                .map(PurchaseItem::getGstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        purchase.setSubtotal(MoneyUtils.roundHalfUp2(subtotal));
        purchase.setGstTotal(MoneyUtils.roundHalfUp2(gstTotal));
        purchase.setGrandTotal(MoneyUtils.roundHalfUp2(subtotal.add(gstTotal)));

        Purchase saved = purchaseRepository.save(purchase);

        auditService.log(tenantId, null, "PURCHASE_CREATED", "Purchase", saved.getId(), null);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // GRN – receive goods
    // -------------------------------------------------------------------------

    /**
     * Record a Goods Receipt Note (GRN) for a purchase.
     *
     * <p>Each line item's quantity is committed to inventory via
     * {@link InventoryService}, and the purchase status advances to
     * {@link PurchaseStatus#BILLED}.</p>
     *
     * @param purchaseId the UUID of the purchase to receive
     * @return the updated purchase as a response DTO
     * @throws BusinessException if the purchase has already been received
     */
    public PurchaseResponse receive(UUID purchaseId) {
        UUID tenantId = TenantContext.require();
        Purchase purchase = findForTenant(purchaseId, tenantId);

        if (purchase.getStatus() != PurchaseStatus.DRAFT
                && purchase.getStatus() != PurchaseStatus.PENDING_GRN) {
            throw new BusinessException(
                    "Purchase " + purchase.getPurchaseNo() + " has already been received (status: " + purchase.getStatus() + ")");
        }

        for (PurchaseItem item : purchase.getItems()) {
            inventoryService.recordMovement(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    MovementReason.GRN,
                    MovementRefType.PURCHASE,
                    purchaseId,
                    "GRN for purchase " + purchase.getPurchaseNo()
            );
        }

        purchase.setStatus(PurchaseStatus.BILLED);
        Purchase saved = purchaseRepository.save(purchase);

        auditService.log(tenantId, null, "PURCHASE_RECEIVED", "Purchase", purchaseId, null);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Mark paid
    // -------------------------------------------------------------------------

    /**
     * Mark a purchase as paid by the business to the supplier.
     *
     * @param purchaseId the UUID of the purchase to settle
     * @return the updated purchase as a response DTO
     * @throws BusinessException if the purchase is not in {@link PurchaseStatus#BILLED} state
     */
    public PurchaseResponse markPaid(UUID purchaseId) {
        UUID tenantId = TenantContext.require();
        Purchase purchase = findForTenant(purchaseId, tenantId);

        if (purchase.getStatus() != PurchaseStatus.BILLED) {
            throw new BusinessException(
                    "Purchase " + purchase.getPurchaseNo() + " cannot be marked paid from status: " + purchase.getStatus());
        }

        purchase.setStatus(PurchaseStatus.PAID);
        Purchase saved = purchaseRepository.save(purchase);

        auditService.log(tenantId, null, "PURCHASE_PAID", "Purchase", purchaseId, null);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * List purchases for a tenant, optionally filtered by status.
     *
     * @param tenantId the tenant UUID
     * @param status   optional status filter; pass {@code null} to return all
     * @param pageable pagination instructions
     * @return a page of purchase responses
     */
    public PageResponse<PurchaseResponse> list(UUID tenantId, PurchaseStatus status, Pageable pageable) {
        Page<Purchase> page = (status != null)
                ? purchaseRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : purchaseRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    /**
     * Fetch a single purchase by its surrogate UUID.
     *
     * @param id the purchase UUID
     * @return the purchase as a response DTO
     * @throws EntityNotFoundException if not found or not owned by current tenant
     */
    public PurchaseResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        return toResponse(findForTenant(id, tenantId));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Purchase findForTenant(UUID id, UUID tenantId) {
        return purchaseRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + id));
    }

    private List<PurchaseItem> buildItems(Purchase purchase, List<PurchaseItemRequest> requests, UUID tenantId) {
        List<PurchaseItem> items = new ArrayList<>(requests.size());
        for (PurchaseItemRequest req : requests) {
            Product product = productRepository.findById(req.productId())
                    .filter(p -> tenantId.equals(p.getTenantId()))
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + req.productId()));

            BigDecimal amount = MoneyUtils.roundHalfUp2(req.quantity().multiply(req.rate()));
            BigDecimal gstAmount = MoneyUtils.pct(amount, req.gstRate());

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setQuantity(req.quantity());
            item.setRate(req.rate());
            item.setGstRate(req.gstRate());
            item.setAmount(amount);
            item.setGstAmount(gstAmount);
            items.add(item);
        }
        return items;
    }

    private PurchaseResponse toResponse(Purchase p) {
        List<PurchaseItemResponse> itemResponses = p.getItems().stream()
                .map(i -> new PurchaseItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getRate(),
                        i.getGstRate(),
                        i.getAmount(),
                        i.getGstAmount()
                ))
                .toList();

        return new PurchaseResponse(
                p.getId(),
                p.getPurchaseNo(),
                p.getSupplier().getId(),
                p.getSupplier().getName(),
                p.getBillDate(),
                p.getDueDate(),
                p.getSubtotal(),
                p.getGstTotal(),
                p.getGrandTotal(),
                p.getStatus(),
                p.getNotes(),
                itemResponses,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
