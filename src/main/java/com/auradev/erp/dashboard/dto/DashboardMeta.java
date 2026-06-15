package com.auradev.erp.dashboard.dto;

public record DashboardMeta(
        String preset,
        String periodLabel,
        String compareLabel,
        String customerName,
        String productName,
        boolean filtersActive
) {}
