package com.auradev.erp.billing.controller;

import com.auradev.erp.billing.dto.CreateCustomerRequest;
import com.auradev.erp.billing.dto.CustomerResponse;
import com.auradev.erp.billing.service.BillingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer directory for billing")
@RequiredArgsConstructor
public class CustomerController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'CUSTOMER_VIEW')")
    public ResponseEntity<List<CustomerResponse>> list() {
        return ResponseEntity.ok(billingService.listCustomers());
    }

    @PostMapping
    @PreAuthorize("@authz.can(authentication, 'BILL_CREATE')")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createCustomer(req));
    }
}
