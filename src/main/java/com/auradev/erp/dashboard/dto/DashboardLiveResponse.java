package com.auradev.erp.dashboard.dto;

import java.util.List;

/** Lightweight dashboard payload for periodic refresh (no sales-trend chart). */
public record DashboardLiveResponse(
        String tenantName,
        String userName,
        DashboardMeta meta,
        DashboardKpis kpis,
        List<TopProductPoint> topProducts,
        List<RecentBillRow> recentBills,
        List<LowStockRow> lowStock,
        List<ActivityRow> activity,
        String aiBrief
) {}
