package com.auradev.erp.billing.controller;

import com.auradev.erp.billing.dto.CustomerResponse;
import com.auradev.erp.billing.service.BillingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomerResponse>> list() {
        return ResponseEntity.ok(billingService.listCustomers());
    }
}
