package com.auradev.erp.analytics.dto;

import java.math.BigDecimal;

/**
 * Snapshot of key trading metrics shown on the tenant's dashboard.
 *
 * @param todaysSales       total revenue from bills raised today
 * @param billsToday        number of bills raised today
 * @param itemsSold         total number of line-item units sold today
 * @param lowStockCount     number of products at or below their reorder level
 * @param salesVsYesterday  percentage change in sales versus the previous day
 *                          (positive = growth, negative = decline)
 * @param billsVsYesterday  absolute change in bill count versus the previous day
 */
public record DashboardSummary(
        BigDecimal todaysSales,
        long billsToday,
        long itemsSold,
        long lowStockCount,
        BigDecimal salesVsYesterday,
        long billsVsYesterday
) {}
