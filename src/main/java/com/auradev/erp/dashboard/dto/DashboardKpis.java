package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;

public record DashboardKpis(
        BigDecimal periodSales,
        BigDecimal compareSales,
        long periodBills,
        long compareBills,
        long periodItems,
        long compareItems,
        int lowStockCount
) {}
