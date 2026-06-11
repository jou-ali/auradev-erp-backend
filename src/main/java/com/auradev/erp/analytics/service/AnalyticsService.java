package com.auradev.erp.analytics.service;

import com.auradev.erp.analytics.dto.DashboardSummary;
import com.auradev.erp.analytics.dto.ReportPoint;
import com.auradev.erp.analytics.dto.SalesTrendPoint;
import com.auradev.erp.analytics.dto.TopProduct;
import com.auradev.erp.analytics.projection.ActivityProjection;
import com.auradev.erp.analytics.projection.ReportPointProjection;
import com.auradev.erp.analytics.projection.SalesTrendProjection;
import com.auradev.erp.analytics.projection.TopProductProjection;
import com.auradev.erp.billing.repository.BillRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for aggregating sales, inventory and activity analytics.
 *
 * <p>Dashboard summary results are cached for 120 seconds per tenant to
 * reduce database load during high-traffic periods.</p>
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BillRepository billRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    // -------------------------------------------------------------------------
    // Dashboard summary
    // -------------------------------------------------------------------------

    /**
     * Build the KPI snapshot for the tenant's dashboard.
     *
     * <p>Results are cached for 120 s under the key
     * {@code analytics:dashboard:{tenantId}}.</p>
     *
     * @param tenantId the tenant UUID
     * @return a populated {@link DashboardSummary}
     */
    @Cacheable(value = "analytics:dashboard", key = "#tenantId", unless = "#result == null")
    public DashboardSummary getDashboardSummary(UUID tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant yesterdayStart = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();

        BigDecimal todaysSales = billRepository.sumGrandTotalByTenantIdAndCreatedAtBetween(
                tenantId, todayStart, todayEnd);
        if (todaysSales == null) todaysSales = BigDecimal.ZERO;

        BigDecimal yesterdaySales = billRepository.sumGrandTotalByTenantIdAndCreatedAtBetween(
                tenantId, yesterdayStart, todayStart);
        if (yesterdaySales == null) yesterdaySales = BigDecimal.ZERO;

        long billsToday = billRepository.countByTenantIdAndCreatedAtBetween(
                tenantId, todayStart, todayEnd);
        long billsYesterday = billRepository.countByTenantIdAndCreatedAtBetween(
                tenantId, yesterdayStart, todayStart);

        BigDecimal itemsQty = billRepository.sumItemsQtyByTenantIdAndCreatedAtBetween(
                tenantId, todayStart, todayEnd);
        if (itemsQty == null) itemsQty = BigDecimal.ZERO;
        long itemsSold = itemsQty.longValue();

        long lowStockCount = productRepository.countLowStockByTenantId(tenantId);

        BigDecimal salesVsYesterday = BigDecimal.ZERO;
        if (yesterdaySales.compareTo(BigDecimal.ZERO) != 0) {
            salesVsYesterday = todaysSales.subtract(yesterdaySales)
                    .multiply(new BigDecimal("100"))
                    .divide(yesterdaySales, 2, RoundingMode.HALF_UP);
        }

        long billsVsYesterday = billsToday - billsYesterday;

        return new DashboardSummary(
                todaysSales,
                billsToday,
                itemsSold,
                lowStockCount,
                salesVsYesterday,
                billsVsYesterday
        );
    }

    // -------------------------------------------------------------------------
    // Sales trend
    // -------------------------------------------------------------------------

    /**
     * Return a sales trend series comparing the current period against the
     * equivalent previous period.
     *
     * @param tenantId the tenant UUID
     * @param range    one of {@code today}, {@code week}, {@code month}
     * @return ordered list of trend data points mapped to {@link SalesTrendPoint}
     */
    public List<SalesTrendPoint> getSalesTrend(UUID tenantId, String range) {
        List<SalesTrendProjection> projections = switch (range) {
            case "today" -> billRepository.salesTrendHourly(tenantId);
            case "week"  -> billRepository.salesTrendDaily(tenantId, 7);
            case "month" -> billRepository.salesTrendDaily(tenantId, 30);
            default      -> billRepository.salesTrendDaily(tenantId, 7);
        };
        return projections.stream()
                .map(p -> new SalesTrendPoint(p.getLabel(), p.getCurrent(), p.getPrevious()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Top products
    // -------------------------------------------------------------------------

    /**
     * Return the top-selling products for a given period and metric.
     *
     * @param tenantId the tenant UUID
     * @param range    one of {@code today}, {@code week}, {@code month}
     * @param metric   one of {@code revenue}, {@code qty}
     * @return top products sorted descending by the requested metric
     */
    public List<TopProduct> getTopProducts(UUID tenantId, String range, String metric) {
        LocalDate from = resolveFromDate(range);
        LocalDate to = LocalDate.now().plusDays(1);
        boolean byRevenue = !"qty".equalsIgnoreCase(metric);
        return billRepository.topProducts(tenantId, from, to, byRevenue, 10).stream()
                .map(p -> new TopProduct(p.getName(), p.getQty(), p.getRevenue()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Flexible report
    // -------------------------------------------------------------------------

    /**
     * Build a flexible aggregated report.
     *
     * @param tenantId the tenant UUID
     * @param metric   the measure: {@code sales}, {@code bills}, {@code items}
     * @param groupBy  the dimension: {@code day}, {@code category},
     *                 {@code product}, {@code payment_method}, {@code cashier}
     * @param from     inclusive start date
     * @param to       inclusive end date
     * @return ordered list of report data points
     */
    public List<ReportPoint> getReport(UUID tenantId, String metric, String groupBy,
                                       LocalDate from, LocalDate to) {
        return billRepository.flexibleReport(tenantId, metric, groupBy, from, to.plusDays(1)).stream()
                .map(p -> new ReportPoint(p.getDimension(), p.getValue(), p.getLabel()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Activity feed
    // -------------------------------------------------------------------------

    /**
     * Return the 20 most recent stock movements and bills combined, sorted by
     * time descending.
     *
     * @param tenantId the tenant UUID
     * @return ordered list of activity items
     */
    public List<ActivityItem> getActivity(UUID tenantId) {
        List<ActivityItem> activities = new ArrayList<>();
        toActivityItems(billRepository.recentBillActivity(tenantId, 10), activities);
        toActivityItems(stockMovementRepository.recentMovementActivity(tenantId, 10), activities);
        activities.sort((a, b) -> b.time().compareTo(a.time()));
        return activities.stream().limit(20).toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void toActivityItems(List<? extends ActivityProjection> projections, List<ActivityItem> out) {
        for (ActivityProjection p : projections) {
            out.add(new ActivityItem(p.getWho(), p.getAction(), p.getDetail(), p.getTime(), p.getEntityType()));
        }
    }

    private LocalDate resolveFromDate(String range) {
        return switch (range) {
            case "today" -> LocalDate.now();
            case "week"  -> LocalDate.now().minusDays(6);
            case "month" -> LocalDate.now().minusDays(29);
            default      -> LocalDate.now().minusDays(6);
        };
    }

    // -------------------------------------------------------------------------
    // Inner record
    // -------------------------------------------------------------------------

    /**
     * A single entry in the activity feed.
     *
     * @param who        display name of the user who performed the action
     * @param action     short verb describing what happened (e.g. "sold", "received")
     * @param detail     human-readable detail (e.g. product name, bill number)
     * @param time       UTC timestamp of the event
     * @param entityType the type of entity involved (e.g. "Bill", "StockMovement")
     */
    public record ActivityItem(
            String who,
            String action,
            String detail,
            Instant time,
            String entityType
    ) {}
}
