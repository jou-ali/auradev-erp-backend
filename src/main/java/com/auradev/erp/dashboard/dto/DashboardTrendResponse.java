package com.auradev.erp.dashboard.dto;

import java.util.List;

public record DashboardTrendResponse(
        List<SalesDayPoint> salesTrend
) {}
