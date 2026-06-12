package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;

public record SalesDayPoint(
        String day,
        BigDecimal current,
        BigDecimal previous
) {}
