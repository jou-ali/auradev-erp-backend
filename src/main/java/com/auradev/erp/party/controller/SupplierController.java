package com.auradev.erp.party.controller;

import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.party.dto.CreateSupplierRequest;
import com.auradev.erp.party.dto.SupplierResponse;
import com.auradev.erp.party.service.SupplierService;
import com.auradev.erp.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for supplier management.
 */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * List all suppliers for the current tenant.
     *
     * @param pageable pagination parameters
     * @return paginated list of suppliers
     */
    @GetMapping
    public ResponseEntity<PageResponse<SupplierResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(supplierService.list(tenantId, pageable));
    }

    /**
     * Create a new supplier.
     *
     * @param req validated request body
     * @return the created supplier with HTTP 201
     */
    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(req));
    }

    /**
     * Fetch a single supplier by UUID.
     *
     * @param id supplier UUID
     * @return the supplier
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.get(id));
    }

    /**
     * Update mutable fields on an existing supplier.
     *
     * @param id  supplier UUID
     * @param req validated update payload
     * @return the updated supplier
     */
    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSupplierRequest req) {
        return ResponseEntity.ok(supplierService.update(id, req));
    }
}
