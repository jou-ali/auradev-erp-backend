package com.auradev.erp.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HeldBillSummaryResponse(
        UUID id,
        String billNo,
        UUID customerId,
        String customerName,
        int itemCount,
        BigDecimal grandTotal,
        Instant updatedAt
) {}
