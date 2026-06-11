package com.auradev.erp.billing.controller;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.billing.dto.BillResponse;
import com.auradev.erp.billing.dto.CreateBillRequest;
import com.auradev.erp.billing.entity.BillStatus;
import com.auradev.erp.billing.service.BillingService;
import com.auradev.erp.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the sales billing module.
 *
 * <h2>Access control</h2>
 * <ul>
 *   <li>POST /   — any authenticated user with CASHIER role or above can create a bill.</li>
 *   <li>GET  /   — any authenticated tenant user can list bills.</li>
 *   <li>GET  /:id — same.</li>
 *   <li>POST /:id/void — MANAGER or ADMIN only.</li>
 *   <li>POST /:id/hold — CASHIER or above.</li>
 * </ul>
 *
 * <h2>Idempotency</h2>
 * <p>Clients may include an {@code Idempotency-Key} HTTP header (or embed the key
 * in the request body) to safely retry failed or timed-out bill creation requests.
 * If a bill with the same key already exists for this tenant it is returned as-is
 * without any further processing.</p>
 */
@RestController
@RequestMapping("/api/v1/bills")
@Tag(name = "Billing", description = "Sales bill management — create, view, void, and hold bills")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // =========================================================================
    // Create bill
    // =========================================================================

    @Operation(
        summary = "Create a new sales bill",
        description = "Validates items, computes GST, deducts stock, and persists the bill atomically."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ACCOUNTANT','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> createBill(
            @Valid @RequestBody CreateBillRequest req,
            @Parameter(description = "Optional idempotency key for safe retries")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        BillResponse response = billingService.createBill(req, principal.getId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // List bills
    // =========================================================================

    @Operation(summary = "List bills for the current tenant", description = "Paginated; optionally filter by status.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<BillResponse>> listBills(
            @Parameter(description = "Filter by bill lifecycle status")
            @RequestParam(required = false) BillStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<BillResponse> page = billingService.listBills(status, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    // =========================================================================
    // Get single bill
    // =========================================================================

    @Operation(summary = "Get a bill by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BillResponse> getBill(
            @Parameter(description = "Bill UUID") @PathVariable UUID id) {

        return ResponseEntity.ok(billingService.getBill(id));
    }

    // =========================================================================
    // Void bill
    // =========================================================================

    @Operation(
        summary = "Void a bill",
        description = "Sets status=VOID, reverses stock movements, writes audit. Requires MANAGER+."
    )
    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('MANAGER','ACCOUNTANT','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> voidBill(
            @Parameter(description = "Bill UUID") @PathVariable UUID id) {

        return ResponseEntity.ok(billingService.voidBill(id));
    }

    // =========================================================================
    // Hold bill
    // =========================================================================

    @Operation(
        summary = "Put a bill on hold",
        description = "Sets status=HELD; stock is NOT reversed."
    )
    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ACCOUNTANT','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> holdBill(
            @Parameter(description = "Bill UUID") @PathVariable UUID id) {

        return ResponseEntity.ok(billingService.holdBill(id));
    }
}
