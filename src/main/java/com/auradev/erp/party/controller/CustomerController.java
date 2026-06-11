package com.auradev.erp.party.controller;

import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.party.dto.CreateCustomerRequest;
import com.auradev.erp.party.dto.CustomerResponse;
import com.auradev.erp.party.service.CustomerService;
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
 * REST controller for customer management.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * List all customers for the current tenant.
     *
     * @param pageable pagination parameters
     * @return paginated list of customers
     */
    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(customerService.list(tenantId, pageable));
    }

    /**
     * Create a new customer.
     *
     * @param req validated request body
     * @return the created customer with HTTP 201
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(req));
    }

    /**
     * Fetch a single customer by UUID.
     *
     * @param id customer UUID
     * @return the customer
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    /**
     * Update mutable fields on an existing customer.
     *
     * @param id  customer UUID
     * @param req validated update payload
     * @return the updated customer
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCustomerRequest req) {
        return ResponseEntity.ok(customerService.update(id, req));
    }
}
