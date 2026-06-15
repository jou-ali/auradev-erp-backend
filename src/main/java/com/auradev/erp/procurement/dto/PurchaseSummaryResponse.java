package com.auradev.erp.procurement.dto;

import com.auradev.erp.procurement.entity.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record PurchaseSummaryResponse(
        UUID id,
        String purchaseNo,
        String supplierName,
        LocalDate billDate,
        LocalDate dueDate,
        int itemCount,
        BigDecimal grandTotal,
        PurchaseStatus status,
        Instant createdAt
) {}
