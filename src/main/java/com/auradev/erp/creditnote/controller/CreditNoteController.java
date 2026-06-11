package com.auradev.erp.creditnote.controller;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.creditnote.dto.CreateCreditNoteRequest;
import com.auradev.erp.creditnote.dto.CreditNoteResponse;
import com.auradev.erp.creditnote.service.CreditNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the credit notes (sales returns) module.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>GET  /api/v1/credit-notes          — paginated list</li>
 *   <li>GET  /api/v1/credit-notes/{id}     — single credit note</li>
 *   <li>GET  /api/v1/credit-notes/by-bill/{billId} — all credit notes for a bill</li>
 *   <li>POST /api/v1/credit-notes          — issue a new credit note</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * <ul>
 *   <li>Creating a credit note requires MANAGER or above.</li>
 *   <li>Reading requires any authenticated user.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/credit-notes")
@Tag(name = "Credit Notes", description = "Sales return / credit note management")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    @Operation(summary = "List credit notes for the current tenant")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CreditNoteResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(creditNoteService.list(pageable));
    }

    @Operation(summary = "Get a credit note by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreditNoteResponse> get(
            @Parameter(description = "Credit note UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(creditNoteService.get(id));
    }

    @Operation(summary = "List all credit notes issued against a specific bill")
    @GetMapping("/by-bill/{billId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CreditNoteResponse>> listByBill(
            @Parameter(description = "Original bill UUID") @PathVariable UUID billId) {
        return ResponseEntity.ok(creditNoteService.listByBill(billId));
    }

    @Operation(
        summary = "Issue a credit note",
        description = "Creates a return against the original bill, restores stock, and credits the customer balance."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER','ACCOUNTANT','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CreditNoteResponse> create(
            @Valid @RequestBody CreateCreditNoteRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditNoteService.create(req, principal.getId()));
    }
}
