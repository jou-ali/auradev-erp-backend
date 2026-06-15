package com.auradev.erp.billing.controller;

import com.auradev.erp.billing.dto.*;
import com.auradev.erp.billing.service.BillingService;
import com.auradev.erp.common.pagination.PageResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bills")
@Tag(name = "Bills", description = "POS billing and sales")
@RequiredArgsConstructor
public class BillController {

    private final BillingService billingService;

    @GetMapping
    @Operation(summary = "List completed bills", description = "Paginated sales bill history")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PageResponse<BillSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(billingService.listCompletedBills(q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bill", description = "Full bill with line items for reprint")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getBill(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> create(@Valid @RequestBody CreateBillRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createBill(req));
    }

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Park bill", description = "Save current cart as HELD — no stock movement or payment until completed")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> hold(@Valid @RequestBody HoldBillRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.holdBill(req));
    }

    @GetMapping("/held")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<HeldBillSummaryResponse>> listHeld() {
        return ResponseEntity.ok(billingService.listHeldBills());
    }

    @GetMapping("/held/{id}")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> getHeld(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getHeldBill(id));
    }

    @PostMapping("/held/{id}/complete")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> completeHeld(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteHeldBillRequest req) {
        return ResponseEntity.ok(billingService.completeHeldBill(id, req));
    }

    @DeleteMapping("/held/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> discardHeld(@PathVariable UUID id) {
        billingService.discardHeldBill(id);
        return ResponseEntity.noContent().build();
    }
}
