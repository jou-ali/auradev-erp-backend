package com.auradev.erp.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        String tenantName,
        String userName,
        DashboardMeta meta,
        DashboardKpis kpis,
        List<SalesDayPoint> salesTrend,
        List<TopProductPoint> topProducts,
        List<RecentBillRow> recentBills,
        List<LowStockRow> lowStock,
        List<ActivityRow> activity,
        String aiBrief
) {}
