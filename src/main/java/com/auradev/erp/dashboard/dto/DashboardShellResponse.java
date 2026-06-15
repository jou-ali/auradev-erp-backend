package com.auradev.erp.dashboard.dto;

import java.util.List;

/** Store-wide dashboard slice — does not depend on period/customer/product filters. */
public record DashboardShellResponse(
        String tenantName,
        String userName,
        int lowStockCount,
        List<LowStockRow> lowStock,
        List<ActivityRow> activity
) {}
