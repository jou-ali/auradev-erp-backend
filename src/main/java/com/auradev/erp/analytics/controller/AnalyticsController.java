package com.auradev.erp.analytics.controller;

import com.auradev.erp.analytics.dto.DashboardSummary;
import com.auradev.erp.analytics.dto.ReportPoint;
import com.auradev.erp.analytics.dto.SalesTrendPoint;
import com.auradev.erp.analytics.dto.TopProduct;
import com.auradev.erp.analytics.service.AnalyticsService;
import com.auradev.erp.analytics.service.AnalyticsService.ActivityItem;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing analytics and dashboard endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Return the KPI snapshot for the current tenant's dashboard.
     *
     * @return populated {@link DashboardSummary}
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(analyticsService.getDashboardSummary(tenantId));
    }

    /**
     * Return a sales trend series.
     *
     * @param range one of {@code today}, {@code week}, {@code month}
     *              (defaults to {@code week})
     * @return ordered list of trend data points
     */
    @GetMapping("/analytics/sales-trend")
    public ResponseEntity<List<SalesTrendPoint>> getSalesTrend(
            @RequestParam(defaultValue = "week") String range) {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(analyticsService.getSalesTrend(tenantId, range));
    }

    /**
     * Return the top-selling products for a period.
     *
     * @param range  one of {@code today}, {@code week}, {@code month}
     *               (defaults to {@code week})
     * @param metric one of {@code revenue}, {@code qty} (defaults to {@code revenue})
     * @return top products sorted descending
     */
    @GetMapping("/analytics/top-products")
    public ResponseEntity<List<TopProduct>> getTopProducts(
            @RequestParam(defaultValue = "week") String range,
            @RequestParam(defaultValue = "revenue") String metric) {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(analyticsService.getTopProducts(tenantId, range, metric));
    }

    /**
     * Run a flexible aggregated report.
     *
     * @param metric  the measure: {@code sales}, {@code bills}, {@code items}
     * @param groupBy the dimension: {@code day}, {@code category},
     *                {@code product}, {@code payment_method}, {@code cashier}
     * @param from    inclusive start date (ISO-8601)
     * @param to      inclusive end date (ISO-8601)
     * @return ordered list of report data points
     */
    @GetMapping("/analytics/report")
    public ResponseEntity<List<ReportPoint>> getReport(
            @RequestParam(defaultValue = "sales") String metric,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(analyticsService.getReport(tenantId, metric, groupBy, from, to));
    }

    /**
     * Return the 20 most recent activity items for the current tenant.
     *
     * @return list of activity items sorted by time descending
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityItem>> getActivity() {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(analyticsService.getActivity(tenantId));
    }
}
