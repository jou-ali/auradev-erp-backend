package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.BillPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillSummaryResponse(
        UUID id,
        String billNo,
        String customerName,
        String cashierName,
        int itemCount,
        BigDecimal grandTotal,
        BillPaymentStatus paymentStatus,
        Instant createdAt
) {}
