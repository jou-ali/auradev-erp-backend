package com.auradev.erp.procurement.controller;

import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.procurement.dto.CreatePurchaseRequest;
import com.auradev.erp.procurement.dto.PurchaseResponse;
import com.auradev.erp.procurement.dto.PurchaseSummaryResponse;
import com.auradev.erp.procurement.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@Tag(name = "Purchases", description = "Supplier purchase bills and GRN")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_VIEW')")
    public ResponseEntity<PageResponse<PurchaseSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID supplierId,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(purchaseService.list(q, status, supplierId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_VIEW')")
    public ResponseEntity<PurchaseResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody CreatePurchaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(req));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    public ResponseEntity<PurchaseResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.confirm(id));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive GRN", description = "Stock in all lines and mark purchase as billed")
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    public ResponseEntity<PurchaseResponse> receive(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.receive(id));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    public ResponseEntity<PurchaseResponse> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.markPaid(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID id) {
        purchaseService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }
}
