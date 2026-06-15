package com.auradev.erp.dashboard.repository;

import com.auradev.erp.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final JdbcTemplate jdbc;

    public DashboardKpis fetchKpis(UUID tenantId, DashboardScope scope) {
        Instant windowFrom = scope.compareFrom();
        Instant windowTo = scope.periodTo();

        if (scope.productId() != null) {
            return fetchProductKpis(tenantId, scope, windowFrom, windowTo);
        }
        return fetchBillKpis(tenantId, scope, windowFrom, windowTo);
    }

    private DashboardKpis fetchBillKpis(UUID tenantId, DashboardScope scope, Instant windowFrom, Instant windowTo) {
        var sql = new StringBuilder("""
                SELECT
                  COALESCE(SUM(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN b.grand_total END), 0) AS period_sales,
                  COALESCE(SUM(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN b.grand_total END), 0) AS compare_sales,
                  COUNT(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN 1 END) AS period_bills,
                  COUNT(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN 1 END) AS compare_bills,
                  (SELECT COALESCE(SUM(bi.quantity), 0)
                   FROM bill_items bi
                   JOIN bills bx ON bx.id = bi.bill_id
                   WHERE bx.tenant_id = ? AND bx.status = 'COMPLETED'
                     AND bx.created_at >= ? AND bx.created_at < ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        args.add(Timestamp.from(scope.compareFrom()));
        args.add(Timestamp.from(scope.compareTo()));
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        args.add(Timestamp.from(scope.compareFrom()));
        args.add(Timestamp.from(scope.compareTo()));
        args.add(tenantId);
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        appendBillFilters(sql, scope, args, true, "bx");
        sql.append(") AS period_items, (SELECT COALESCE(SUM(bi.quantity), 0) FROM bill_items bi JOIN bills bx ON bx.id = bi.bill_id WHERE bx.tenant_id = ? AND bx.status = 'COMPLETED' AND bx.created_at >= ? AND bx.created_at < ? ");
        args.add(tenantId);
        args.add(Timestamp.from(scope.compareFrom()));
        args.add(Timestamp.from(scope.compareTo()));
        appendBillFilters(sql, scope, args, true, "bx");
        sql.append("""
                ) AS compare_items
                FROM bills b
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ? AND b.created_at < ?
                """);
        args.add(tenantId);
        args.add(Timestamp.from(windowFrom));
        args.add(Timestamp.from(windowTo));
        appendBillFilters(sql, scope, args);

        var billStats = jdbc.queryForObject(sql.toString(),
                (rs, rowNum) -> new Object[] {
                        rs.getBigDecimal("period_sales"),
                        rs.getBigDecimal("compare_sales"),
                        rs.getLong("period_bills"),
                        rs.getLong("compare_bills"),
                        toWholeUnits(rs.getBigDecimal("period_items")),
                        toWholeUnits(rs.getBigDecimal("compare_items")),
                },
                args.toArray());

        return new DashboardKpis(
                (BigDecimal) billStats[0],
                (BigDecimal) billStats[1],
                (Long) billStats[2],
                (Long) billStats[3],
                (Long) billStats[4],
                (Long) billStats[5],
                countLowStock(tenantId));
    }

    private DashboardKpis fetchProductKpis(UUID tenantId, DashboardScope scope, Instant windowFrom, Instant windowTo) {
        var sql = new StringBuilder("""
                SELECT
                  COALESCE(SUM(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN bi.line_total END), 0) AS period_sales,
                  COALESCE(SUM(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN bi.line_total END), 0) AS compare_sales,
                  COUNT(DISTINCT CASE WHEN b.created_at >= ? AND b.created_at < ? THEN b.id END) AS period_bills,
                  COUNT(DISTINCT CASE WHEN b.created_at >= ? AND b.created_at < ? THEN b.id END) AS compare_bills,
                  COALESCE(SUM(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN bi.quantity END), 0) AS period_items,
                  COALESCE(SUM(CASE WHEN b.created_at >= ? AND b.created_at < ? THEN bi.quantity END), 0) AS compare_items
                FROM bill_items bi
                JOIN bills b ON b.id = bi.bill_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND bi.product_id = ?
                  AND b.created_at >= ? AND b.created_at < ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        args.add(Timestamp.from(scope.compareFrom()));
        args.add(Timestamp.from(scope.compareTo()));
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        args.add(Timestamp.from(scope.compareFrom()));
        args.add(Timestamp.from(scope.compareTo()));
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        args.add(Timestamp.from(scope.compareFrom()));
        args.add(Timestamp.from(scope.compareTo()));
        args.add(tenantId);
        args.add(scope.productId());
        args.add(Timestamp.from(windowFrom));
        args.add(Timestamp.from(windowTo));
        appendBillFilters(sql, scope, args, false);

        return jdbc.queryForObject(sql.toString(),
                (rs, rowNum) -> new DashboardKpis(
                        rs.getBigDecimal("period_sales"),
                        rs.getBigDecimal("compare_sales"),
                        rs.getLong("period_bills"),
                        rs.getLong("compare_bills"),
                        toWholeUnits(rs.getBigDecimal("period_items")),
                        toWholeUnits(rs.getBigDecimal("compare_items")),
                        countLowStock(tenantId)),
                args.toArray());
    }

    public List<SalesDayPoint> salesTrend(UUID tenantId, DashboardScope scope) {
        LocalDate periodStart = LocalDate.ofInstant(scope.periodFrom(), STORE_ZONE);
        LocalDate periodEnd = LocalDate.ofInstant(scope.periodTo(), STORE_ZONE).minusDays(1);
        LocalDate compareStart = LocalDate.ofInstant(scope.compareFrom(), STORE_ZONE);

        long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        int pointCount = scope.trendPointCount();
        int step = (int) Math.max(1, Math.ceil((double) periodDays / pointCount));

        Instant fetchFrom = scope.compareFrom();
        Instant fetchTo = scope.periodTo();
        Map<LocalDate, BigDecimal> salesByDay = fetchDailySales(tenantId, scope, fetchFrom, fetchTo);

        List<SalesDayPoint> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            long offset = Math.min(i * step, periodDays - 1);
            LocalDate day = periodStart.plusDays(offset);
            LocalDate prevDay = compareStart.plusDays(offset);
            String label = formatTrendLabel(day, scope.preset(), periodDays);
            points.add(new SalesDayPoint(
                    label,
                    salesByDay.getOrDefault(day, ZERO),
                    salesByDay.getOrDefault(prevDay, ZERO)));
        }
        return points;
    }

    private static String formatTrendLabel(LocalDate day, String preset, long periodDays) {
        if (periodDays == 1) {
            return switch (preset != null ? preset : "") {
                case "yesterday" -> "Yesterday";
                case "today" -> "Today";
                default -> day.getDayOfMonth() + " " + day.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            };
        }
        if (periodDays <= 7) {
            return day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }
        if (periodDays <= 14) {
            return String.valueOf(day.getDayOfMonth());
        }
        return day.getDayOfMonth() + " " + day.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private Map<LocalDate, BigDecimal> fetchDailySales(UUID tenantId, DashboardScope scope, Instant from, Instant to) {
        if (scope.productId() != null) {
            return fetchDailyProductSales(tenantId, scope, from, to);
        }

        var sql = new StringBuilder("""
                SELECT (b.created_at AT TIME ZONE 'Asia/Kolkata')::date AS day,
                       COALESCE(SUM(b.grand_total), 0) AS total
                FROM bills b
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ? AND b.created_at < ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(Timestamp.from(from));
        args.add(Timestamp.from(to));
        appendBillFilters(sql, scope, args);
        sql.append(" GROUP BY 1");

        return mapDailySales(sql.toString(), args);
    }

    private Map<LocalDate, BigDecimal> fetchDailyProductSales(UUID tenantId, DashboardScope scope, Instant from, Instant to) {
        var sql = new StringBuilder("""
                SELECT (b.created_at AT TIME ZONE 'Asia/Kolkata')::date AS day,
                       COALESCE(SUM(bi.line_total), 0) AS total
                FROM bill_items bi
                JOIN bills b ON b.id = bi.bill_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND bi.product_id = ?
                  AND b.created_at >= ? AND b.created_at < ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(scope.productId());
        args.add(Timestamp.from(from));
        args.add(Timestamp.from(to));
        appendBillFilters(sql, scope, args, false);
        sql.append(" GROUP BY 1");

        return mapDailySales(sql.toString(), args);
    }

    private Map<LocalDate, BigDecimal> mapDailySales(String sql, List<Object> args) {
        List<Map.Entry<LocalDate, BigDecimal>> rows = jdbc.query(sql,
                (rs, rowNum) -> Map.entry(
                        rs.getDate("day").toLocalDate(),
                        rs.getBigDecimal("total")),
                args.toArray());

        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (Map.Entry<LocalDate, BigDecimal> row : rows) {
            map.put(row.getKey(), row.getValue());
        }
        return map;
    }

    public List<UUID> topSellingProductIds(UUID tenantId, int limit, int days) {
        Instant from = LocalDate.now(STORE_ZONE).minusDays(days)
                .atStartOfDay(STORE_ZONE).toInstant();
        return jdbc.query("""
                SELECT bi.product_id
                FROM bill_items bi
                JOIN bills b ON b.id = bi.bill_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ?
                GROUP BY bi.product_id
                ORDER BY SUM(bi.quantity) DESC
                LIMIT ?
                """,
                (rs, rowNum) -> UUID.fromString(rs.getString("product_id")),
                tenantId, Timestamp.from(from), limit);
    }

    public List<TopProductPoint> topProductsInScope(UUID tenantId, DashboardScope scope, int limit) {
        var sql = new StringBuilder("""
                SELECT bi.product_name_snapshot,
                       COALESCE(SUM(bi.quantity), 0) AS qty,
                       COALESCE(SUM(bi.line_total), 0) AS rev
                FROM bill_items bi
                JOIN bills b ON b.id = bi.bill_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ? AND b.created_at < ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        appendBillFilters(sql, scope, args);
        if (scope.productId() != null) {
            sql.append(" AND bi.product_id = ?");
            args.add(scope.productId());
        }
        sql.append("""
                 GROUP BY bi.product_id, bi.product_name_snapshot
                 ORDER BY rev DESC
                 LIMIT ?
                """);
        args.add(limit);

        return jdbc.query(sql.toString(),
                (rs, rowNum) -> new TopProductPoint(
                        rs.getString("product_name_snapshot"),
                        rs.getLong("qty"),
                        rs.getBigDecimal("rev")),
                args.toArray());
    }

    public List<RecentBillRow> recentBillsInScope(UUID tenantId, DashboardScope scope, int limit) {
        var sql = new StringBuilder("""
                SELECT b.bill_no,
                       c.name AS customer,
                       u.name AS cashier,
                       COALESCE(items.cnt, 0) AS items,
                       b.grand_total,
                       COALESCE(pay.method::text, 'CASH') AS payment,
                       b.payment_status::text AS payment_status,
                       b.created_at
                FROM bills b
                JOIN customers c ON c.id = b.customer_id
                JOIN users u ON u.id = b.cashier_id
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::int AS cnt FROM bill_items bi WHERE bi.bill_id = b.id
                ) items ON TRUE
                LEFT JOIN LATERAL (
                    SELECT p.method FROM payments p
                    WHERE p.bill_id = b.id
                    ORDER BY p.amount DESC
                    LIMIT 1
                ) pay ON TRUE
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ? AND b.created_at < ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(Timestamp.from(scope.periodFrom()));
        args.add(Timestamp.from(scope.periodTo()));
        appendBillFilters(sql, scope, args);
        sql.append(" ORDER BY b.created_at DESC LIMIT ?");
        args.add(limit);

        return jdbc.query(sql.toString(), this::mapRecentBill, args.toArray());
    }

    private RecentBillRow mapRecentBill(ResultSet rs, int rowNum) throws SQLException {
        return new RecentBillRow(
                rs.getString("bill_no"),
                rs.getString("customer"),
                rs.getString("cashier"),
                rs.getInt("items"),
                rs.getBigDecimal("grand_total"),
                formatPayment(rs.getString("payment")),
                rs.getString("payment_status"),
                rs.getTimestamp("created_at").toInstant());
    }

    public int countLowStock(UUID tenantId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)::int
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                WHERE i.tenant_id = ? AND p.is_active = TRUE
                  AND (
                    i.quantity_on_hand <= 0
                    OR (i.low_stock_threshold IS NOT NULL AND i.quantity_on_hand <= i.low_stock_threshold)
                  )
                """, Integer.class, tenantId);
        return count != null ? count : 0;
    }

    public List<LowStockRow> lowStockProducts(UUID tenantId, int limit) {
        return jdbc.query("""
                SELECT p.id,
                       p.name,
                       p.sku,
                       COALESCE(c.name, 'Uncategorised') AS category,
                       p.unit_label,
                       i.quantity_on_hand,
                       COALESCE(i.reorder_quantity, i.low_stock_threshold, 0) AS reorder_qty,
                       CASE
                         WHEN i.quantity_on_hand <= 0 THEN 'OUT'
                         ELSE 'LOW'
                       END AS stock_status
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                LEFT JOIN categories c ON c.id = p.category_id
                WHERE i.tenant_id = ? AND p.is_active = TRUE
                  AND (
                    i.quantity_on_hand <= 0
                    OR (i.low_stock_threshold IS NOT NULL AND i.quantity_on_hand <= i.low_stock_threshold)
                  )
                ORDER BY i.quantity_on_hand ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new LowStockRow(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getString("category"),
                        rs.getString("unit_label"),
                        rs.getBigDecimal("quantity_on_hand"),
                        rs.getBigDecimal("reorder_qty"),
                        rs.getString("stock_status")),
                tenantId, limit);
    }

    public List<ActivityRow> recentActivity(UUID tenantId, int limit) {
        return jdbc.query("""
                SELECT COALESCE(u.name, 'System') AS who,
                       sm.movement_type::text AS movement_type,
                       p.name AS product_name,
                       p.sku,
                       sm.quantity,
                       sm.notes,
                       sm.created_at
                FROM stock_movements sm
                JOIN products p ON p.id = sm.product_id
                LEFT JOIN users u ON u.id = sm.created_by
                WHERE sm.tenant_id = ?
                ORDER BY sm.created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> mapActivity(
                        rs.getString("who"),
                        rs.getString("movement_type"),
                        rs.getString("product_name"),
                        rs.getString("sku"),
                        rs.getBigDecimal("quantity"),
                        rs.getString("notes"),
                        rs.getTimestamp("created_at").toInstant()),
                tenantId, limit);
    }

    private static long toWholeUnits(BigDecimal qty) {
        if (qty == null) return 0L;
        return qty.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    private static void appendBillFilters(StringBuilder sql, DashboardScope scope, List<Object> args) {
        appendBillFilters(sql, scope, args, true, "b");
    }

    private static void appendBillFilters(StringBuilder sql, DashboardScope scope, List<Object> args, boolean includeProductExists) {
        appendBillFilters(sql, scope, args, includeProductExists, "b");
    }

    private static void appendBillFilters(
            StringBuilder sql,
            DashboardScope scope,
            List<Object> args,
            boolean includeProductExists,
            String billAlias) {
        if (scope.customerId() != null) {
            sql.append(" AND ").append(billAlias).append(".customer_id = ?");
            args.add(scope.customerId());
        }
        if (includeProductExists && scope.productId() != null) {
            sql.append("""
                     AND EXISTS (
                       SELECT 1 FROM bill_items bi_f
                       WHERE bi_f.bill_id = """)
                    .append(billAlias)
                    .append(".id AND bi_f.product_id = ?")
                    .append("""
                     )
                    """);
            args.add(scope.productId());
        }
    }

    private static ActivityRow mapActivity(
            String who, String movementType, String productName, String sku,
            BigDecimal quantity, String notes, Instant createdAt) {

        String action = switch (movementType) {
            case "sale" -> "sold";
            case "purchase" -> "received stock";
            case "return", "customer_return" -> "processed return";
            case "waste" -> "recorded wastage";
            case "adjustment_in" -> "adjusted stock";
            case "adjustment_out" -> "adjusted stock";
            default -> "updated stock";
        };

        String detail = productName + " · " + sku;
        if (notes != null && !notes.isBlank()) {
            detail += " · " + notes;
        } else {
            detail += " · " + quantity.stripTrailingZeros().toPlainString();
        }

        String icon = switch (movementType) {
            case "sale" -> "receipt";
            case "purchase" -> "package-plus";
            case "return", "customer_return" -> "undo-2";
            case "waste" -> "trash-2";
            default -> "package";
        };

        String tone = switch (movementType) {
            case "sale" -> "primary";
            case "purchase", "adjustment_in" -> "success";
            case "waste", "adjustment_out" -> "warning";
            case "return", "customer_return" -> "info";
            default -> "neutral";
        };

        return new ActivityRow(who, action, detail, icon, tone, createdAt);
    }

    private static String formatPayment(String method) {
        if (method == null) return "Cash";
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "UPI" -> "UPI";
            case "CARD" -> "Card";
            case "CREDIT" -> "Credit";
            case "SPLIT_COMPONENT" -> "Split";
            default -> "Cash";
        };
    }
}
