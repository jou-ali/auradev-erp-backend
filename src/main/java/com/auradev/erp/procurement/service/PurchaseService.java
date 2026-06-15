package com.auradev.erp.procurement.service;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.entity.Supplier;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.catalog.repository.SupplierRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.common.util.MoneyUtils;
import com.auradev.erp.inventory.entity.MovementType;
import com.auradev.erp.inventory.entity.RefType;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.procurement.dto.*;
import com.auradev.erp.procurement.entity.*;
import com.auradev.erp.procurement.repository.PurchaseRepository;
import com.auradev.erp.procurement.repository.PurchaseSequenceRepository;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final PurchaseRepository purchaseRepository;
    private final PurchaseSequenceRepository sequenceRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseSummaryResponse> list(
            String q, String status, UUID supplierId, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        PurchaseStatus statusFilter = parseStatus(status);
        var page = purchaseRepository.searchSummaries(
                tenantId, blankToNull(q), statusFilter, supplierId, pageable);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        Purchase purchase = purchaseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase", id));
        Supplier supplier = supplierRepository.findById(purchase.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier", purchase.getSupplierId()));
        return toResponse(purchase, supplier);
    }

    public PurchaseResponse create(CreatePurchaseRequest req) {
        UUID tenantId = TenantContext.require();
        UUID userId = requirePrincipal().getId();

        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier", req.supplierId()));
        if (!supplier.isActive()) {
            throw new BusinessException("SUPPLIER_INACTIVE", "Supplier is inactive: " + supplier.getName());
        }

        List<ComputedLine> lines = computeLines(req.items());
        Totals totals = sumTotals(lines);

        Purchase purchase = new Purchase();
        purchase.setTenantId(tenantId);
        purchase.setPurchaseNo(nextPurchaseNo(tenantId));
        purchase.setSupplierId(supplier.getId());
        purchase.setBillDate(req.billDate());
        purchase.setDueDate(req.dueDate());
        purchase.setSubtotal(totals.subtotal());
        purchase.setGstTotal(totals.gstTotal());
        purchase.setGrandTotal(totals.grandTotal());
        purchase.setStatus(PurchaseStatus.DRAFT);
        purchase.setNotes(req.notes());
        purchase.setCreatedBy(userId);
        purchase.setUpdatedBy(userId);

        for (ComputedLine cl : lines) {
            purchase.getItems().add(toItem(purchase, cl));
        }

        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved, supplier);
    }

    public PurchaseResponse confirm(UUID id) {
        Purchase purchase = loadDraftOrPending(id);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Only draft purchases can be confirmed");
        }
        purchase.setStatus(PurchaseStatus.PENDING_GRN);
        purchase.setUpdatedBy(requirePrincipal().getId());
        return toResponse(purchaseRepository.save(purchase), loadSupplier(purchase.getSupplierId()));
    }

    public PurchaseResponse receive(UUID id) {
        UUID tenantId = TenantContext.require();
        Purchase purchase = purchaseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase", id));

        if (purchase.getStatus() != PurchaseStatus.DRAFT
                && purchase.getStatus() != PurchaseStatus.PENDING_GRN) {
            throw new BusinessException("INVALID_STATUS", "Purchase already received or closed");
        }

        for (PurchaseItem item : purchase.getItems()) {
            inventoryService.recordMovement(
                    item.getProduct().getId(),
                    MovementType.purchase,
                    item.getQuantity(),
                    RefType.purchase_order,
                    purchase.getId(),
                    "GRN " + purchase.getPurchaseNo());

            Product product = item.getProduct();
            product.setCostPrice(item.getRate());
            productRepository.save(product);
        }

        purchase.setStatus(PurchaseStatus.BILLED);
        purchase.setUpdatedBy(requirePrincipal().getId());
        return toResponse(purchaseRepository.save(purchase), loadSupplier(purchase.getSupplierId()));
    }

    public PurchaseResponse markPaid(UUID id) {
        UUID tenantId = TenantContext.require();
        Purchase purchase = purchaseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase", id));

        if (purchase.getStatus() != PurchaseStatus.BILLED) {
            throw new BusinessException("INVALID_STATUS", "Only billed purchases can be marked paid");
        }

        purchase.setStatus(PurchaseStatus.PAID);
        purchase.setUpdatedBy(requirePrincipal().getId());
        return toResponse(purchaseRepository.save(purchase), loadSupplier(purchase.getSupplierId()));
    }

    private Purchase loadDraftOrPending(UUID id) {
        UUID tenantId = TenantContext.require();
        return purchaseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase", id));
    }

    private Supplier loadSupplier(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier", supplierId));
    }

    private List<ComputedLine> computeLines(List<PurchaseLineRequest> items) {
        List<ComputedLine> result = new ArrayList<>();
        for (PurchaseLineRequest line : items) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Product", line.productId()));
            if (!product.isActive()) {
                throw new BusinessException("PRODUCT_INACTIVE", "Product is inactive: " + product.getName());
            }

            BigDecimal qty = MoneyUtils.roundHalfUp3(line.quantity());
            BigDecimal rate = MoneyUtils.roundHalfUp2(line.rate());
            BigDecimal gstRate = line.gstRatePct() != null
                    ? line.gstRatePct()
                    : product.getTaxRatePct();
            BigDecimal amount = MoneyUtils.roundHalfUp2(rate.multiply(qty));
            BigDecimal gstAmount = MoneyUtils.pct(amount, gstRate);

            result.add(new ComputedLine(product, qty, rate, gstRate, amount, gstAmount));
        }
        return result;
    }

    private Totals sumTotals(List<ComputedLine> lines) {
        BigDecimal subtotal = lines.stream()
                .map(ComputedLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gstTotal = lines.stream()
                .map(ComputedLine::gstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Totals(
                MoneyUtils.roundHalfUp2(subtotal),
                MoneyUtils.roundHalfUp2(gstTotal),
                MoneyUtils.roundHalfUp2(subtotal.add(gstTotal)));
    }

    private PurchaseItem toItem(Purchase purchase, ComputedLine cl) {
        PurchaseItem item = new PurchaseItem();
        item.setPurchase(purchase);
        item.setProduct(cl.product());
        item.setQuantity(cl.quantity());
        item.setRate(cl.rate());
        item.setGstRate(cl.gstRate());
        item.setAmount(cl.amount());
        item.setGstAmount(cl.gstAmount());
        return item;
    }

    private String nextPurchaseNo(UUID tenantId) {
        String fy = String.valueOf(Instant.now().atZone(IST).getYear());
        PurchaseSequenceId seqId = new PurchaseSequenceId(tenantId, fy);
        PurchaseSequence seq = sequenceRepository.findForUpdate(tenantId, fy)
                .orElseGet(() -> {
                    PurchaseSequence created = new PurchaseSequence();
                    created.setId(seqId);
                    created.setLastNo(0);
                    return created;
                });
        seq.setLastNo(seq.getLastNo() + 1);
        sequenceRepository.save(seq);
        return "PB-" + fy + "-" + String.format("%05d", seq.getLastNo());
    }

    private PurchaseResponse toResponse(Purchase purchase, Supplier supplier) {
        List<PurchaseLineResponse> lines = purchase.getItems().stream()
                .map(i -> new PurchaseLineResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getSku(),
                        i.getProduct().getUnitLabel(),
                        i.getQuantity(),
                        i.getRate(),
                        i.getGstRate(),
                        i.getAmount(),
                        i.getGstAmount(),
                        MoneyUtils.roundHalfUp2(i.getAmount().add(i.getGstAmount()))))
                .toList();

        return new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseNo(),
                supplier.getId(),
                supplier.getName(),
                supplier.getGstin(),
                supplier.getPhone(),
                purchase.getBillDate(),
                purchase.getDueDate(),
                purchase.getStatus(),
                purchase.getSubtotal(),
                purchase.getGstTotal(),
                purchase.getGrandTotal(),
                purchase.getNotes(),
                purchase.getCreatedAt(),
                purchase.getUpdatedAt(),
                lines);
    }

    private static PurchaseStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return PurchaseStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return switch (raw.toLowerCase()) {
                case "draft" -> PurchaseStatus.DRAFT;
                case "pending", "pending_grn" -> PurchaseStatus.PENDING_GRN;
                case "billed" -> PurchaseStatus.BILLED;
                case "paid" -> PurchaseStatus.PAID;
                default -> throw new BusinessException("INVALID_STATUS", "Unknown status filter: " + raw);
            };
        }
    }

    private static String blankToNull(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    private UserPrincipal requirePrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        throw new BusinessException("UNAUTHENTICATED", "User context required");
    }

    private record ComputedLine(
            Product product,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal gstRate,
            BigDecimal amount,
            BigDecimal gstAmount) {}

    private record Totals(BigDecimal subtotal, BigDecimal gstTotal, BigDecimal grandTotal) {}
}
