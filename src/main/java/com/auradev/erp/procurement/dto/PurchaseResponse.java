package com.auradev.erp.procurement.dto;

import com.auradev.erp.procurement.entity.PurchaseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        String purchaseNo,
        UUID supplierId,
        String supplierName,
        String supplierGstin,
        String supplierPhone,
        LocalDate billDate,
        LocalDate dueDate,
        PurchaseStatus status,
        BigDecimal subtotal,
        BigDecimal gstTotal,
        BigDecimal grandTotal,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<PurchaseLineResponse> lines
) {}
