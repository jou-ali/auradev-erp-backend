package com.auradev.erp.dashboard.repository;

import com.auradev.erp.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Kolkata");

    private final JdbcTemplate jdbc;

    public BigDecimal sumSalesBetween(UUID tenantId, Instant from, Instant to) {
        BigDecimal total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(grand_total), 0)
                FROM bills
                WHERE tenant_id = ? AND status = 'COMPLETED'
                  AND created_at >= ? AND created_at < ?
                """, BigDecimal.class, tenantId, Timestamp.from(from), Timestamp.from(to));
        return total != null ? total : BigDecimal.ZERO;
    }

    public long countBillsBetween(UUID tenantId, Instant from, Instant to) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM bills
                WHERE tenant_id = ? AND status = 'COMPLETED'
                  AND created_at >= ? AND created_at < ?
                """, Long.class, tenantId, Timestamp.from(from), Timestamp.from(to));
        return count != null ? count : 0L;
    }

    public long sumItemsSoldBetween(UUID tenantId, Instant from, Instant to) {
        Long total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(bi.quantity), 0)
                FROM bill_items bi
                JOIN bills b ON b.id = bi.bill_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ? AND b.created_at < ?
                """, Long.class, tenantId, Timestamp.from(from), Timestamp.from(to));
        return total != null ? total.longValue() : 0L;
    }

    public List<SalesDayPoint> salesTrend(UUID tenantId, int days) {
        List<SalesDayPoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now(STORE_ZONE);

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDate prevDay = day.minusDays(days);

            Instant curFrom = day.atStartOfDay(STORE_ZONE).toInstant();
            Instant curTo = day.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();
            Instant prevFrom = prevDay.atStartOfDay(STORE_ZONE).toInstant();
            Instant prevTo = prevDay.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();

            String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            points.add(new SalesDayPoint(
                    label,
                    sumSalesBetween(tenantId, curFrom, curTo),
                    sumSalesBetween(tenantId, prevFrom, prevTo)
            ));
        }
        return points;
    }

    public List<TopProductPoint> topProductsToday(UUID tenantId, int limit) {
        LocalDate today = LocalDate.now(STORE_ZONE);
        Instant from = today.atStartOfDay(STORE_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();

        return jdbc.query("""
                SELECT bi.product_name_snapshot,
                       COALESCE(SUM(bi.quantity), 0) AS qty,
                       COALESCE(SUM(bi.line_total), 0) AS rev
                FROM bill_items bi
                JOIN bills b ON b.id = bi.bill_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                  AND b.created_at >= ? AND b.created_at < ?
                GROUP BY bi.product_id, bi.product_name_snapshot
                ORDER BY rev DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new TopProductPoint(
                        rs.getString("product_name_snapshot"),
                        rs.getLong("qty"),
                        rs.getBigDecimal("rev")),
                tenantId, Timestamp.from(from), Timestamp.from(to), limit);
    }

    public List<RecentBillRow> recentBills(UUID tenantId, int limit) {
        return jdbc.query("""
                SELECT b.bill_no,
                       c.name AS customer,
                       u.name AS cashier,
                       (SELECT COUNT(*)::int FROM bill_items bi WHERE bi.bill_id = b.id) AS items,
                       b.grand_total,
                       COALESCE(
                           (SELECT p.method::text FROM payments p WHERE p.bill_id = b.id ORDER BY p.amount DESC LIMIT 1),
                           'CASH'
                       ) AS payment,
                       b.payment_status::text AS payment_status,
                       b.created_at
                FROM bills b
                JOIN customers c ON c.id = b.customer_id
                JOIN users u ON u.id = b.cashier_id
                WHERE b.tenant_id = ? AND b.status = 'COMPLETED'
                ORDER BY b.created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new RecentBillRow(
                        rs.getString("bill_no"),
                        rs.getString("customer"),
                        rs.getString("cashier"),
                        rs.getInt("items"),
                        rs.getBigDecimal("grand_total"),
                        formatPayment(rs.getString("payment")),
                        rs.getString("payment_status"),
                        rs.getTimestamp("created_at").toInstant()),
                tenantId, limit);
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
