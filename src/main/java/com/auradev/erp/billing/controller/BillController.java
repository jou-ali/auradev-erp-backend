package com.auradev.erp.billing.controller;

import com.auradev.erp.billing.dto.BillResponse;
import com.auradev.erp.billing.dto.CreateBillRequest;
import com.auradev.erp.billing.service.BillingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bills")
@Tag(name = "Bills", description = "POS billing and sales")
@RequiredArgsConstructor
public class BillController {

    private final BillingService billingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<BillResponse> create(@Valid @RequestBody CreateBillRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createBill(req));
    }
}
