package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;

public record DashboardKpis(
        BigDecimal todaySales,
        BigDecimal yesterdaySales,
        long billsToday,
        long billsYesterday,
        long itemsSoldToday,
        long itemsSoldYesterday,
        int lowStockCount
) {}
