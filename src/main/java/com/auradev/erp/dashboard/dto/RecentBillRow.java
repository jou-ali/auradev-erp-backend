package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentBillRow(
        String billNo,
        String customer,
        String cashier,
        int items,
        BigDecimal total,
        String payment,
        String status,
        Instant createdAt
) {}
