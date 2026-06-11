package com.auradev.erp.purchase.controller;

import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.purchase.dto.CreatePurchaseRequest;
import com.auradev.erp.purchase.dto.PurchaseResponse;
import com.auradev.erp.purchase.entity.PurchaseStatus;
import com.auradev.erp.purchase.service.PurchaseService;
import com.auradev.erp.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for purchase / supplier-bill operations.
 *
 * <p>All endpoints require one of the following roles:
 * {@code TENANT_ADMIN}, {@code MANAGER}, {@code INVENTORY_STAFF}, or
 * {@code SUPER_ADMIN}.</p>
 */
@RestController
@RequestMapping("/api/v1/purchases")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','MANAGER','INVENTORY_STAFF','SUPER_ADMIN')")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * List purchases for the current tenant, optionally filtered by status.
     *
     * @param status   optional lifecycle status filter
     * @param pageable pagination parameters (default size = 20)
     * @return paginated list of purchases
     */
    @GetMapping
    public ResponseEntity<PageResponse<PurchaseResponse>> list(
            @RequestParam(required = false) PurchaseStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(purchaseService.list(tenantId, status, pageable));
    }

    /**
     * Create a new purchase in DRAFT state.
     *
     * @param req validated request body
     * @return the created purchase with HTTP 201
     */
    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody CreatePurchaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(req));
    }

    /**
     * Fetch a single purchase by its UUID.
     *
     * @param id purchase UUID
     * @return the purchase
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.get(id));
    }

    /**
     * Record a Goods Receipt Note (GRN) — receive goods and advance status to BILLED.
     *
     * @param id purchase UUID
     * @return the updated purchase
     */
    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseResponse> receive(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.receive(id));
    }

    /**
     * Mark a purchase as paid to the supplier.
     *
     * @param id purchase UUID
     * @return the updated purchase
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<PurchaseResponse> pay(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.markPaid(id));
    }
}
