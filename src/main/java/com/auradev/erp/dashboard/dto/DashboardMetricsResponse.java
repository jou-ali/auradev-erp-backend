package com.auradev.erp.dashboard.dto;

import java.util.List;

/** Filter-dependent dashboard slice — reload when period or dimensions change. */
public record DashboardMetricsResponse(
        DashboardMeta meta,
        DashboardKpis kpis,
        List<SalesDayPoint> salesTrend,
        List<TopProductPoint> topProducts,
        List<RecentBillRow> recentBills,
        String aiBrief
) {}
