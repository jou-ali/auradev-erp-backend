package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecentBillRow(
        UUID id,
        String billNo,
        String customer,
        String cashier,
        int items,
        BigDecimal total,
        String payment,
        String status,
        Instant createdAt
) {}
