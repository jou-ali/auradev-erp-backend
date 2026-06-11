package com.auradev.erp.billing.repository;

import com.auradev.erp.analytics.projection.ActivityProjection;
import com.auradev.erp.analytics.projection.ReportPointProjection;
import com.auradev.erp.analytics.projection.SalesTrendProjection;
import com.auradev.erp.analytics.projection.TopProductProjection;
import com.auradev.erp.billing.entity.Bill;
import com.auradev.erp.billing.entity.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link Bill} entities.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    /**
     * Find a bill by its idempotency key and tenant, used for replay detection.
     */
    Optional<Bill> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);

    /**
     * Paginated list of bills for a tenant, excluding the given status.
     * Typical use: list active bills by passing {@link BillStatus#VOID} to
     * suppress voided records.
     */
    Page<Bill> findByTenantIdAndStatusNot(UUID tenantId, BillStatus status, Pageable pageable);

    /**
     * Locate a bill by its sequential bill number within a tenant.
     * Used for receipt printing, void flows, and customer-facing lookup.
     */
    Optional<Bill> findByTenantIdAndBillNo(UUID tenantId, String billNo);

    /**
     * Count of non-voided bills raised today for the given tenant.
     */
    @Query("""
            SELECT COUNT(b) FROM Bill b
            WHERE b.tenantId = :tenantId
              AND b.status   <> com.auradev.erp.billing.entity.BillStatus.VOID
              AND b.createdAt >= :startOfDay
              AND b.createdAt <  :endOfDay
            """)
    long countBillsToday(
            @Param("tenantId") UUID tenantId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay);

    /**
     * Sum of {@code grandTotal} for all non-voided bills raised today.
     * Returns {@code null} if there are no bills today; callers should coerce to ZERO.
     */
    @Query("""
            SELECT SUM(b.grandTotal) FROM Bill b
            WHERE b.tenantId = :tenantId
              AND b.status   <> com.auradev.erp.billing.entity.BillStatus.VOID
              AND b.createdAt >= :startOfDay
              AND b.createdAt <  :endOfDay
            """)
    BigDecimal sumGrandTotalToday(
            @Param("tenantId") UUID tenantId,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay);

    /**
     * Paginated list of bills for a tenant, optionally filtered by status.
     */
    Page<Bill> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Paginated list of bills filtered by tenant and status.
     */
    Page<Bill> findByTenantIdAndStatus(UUID tenantId, BillStatus status, Pageable pageable);

    /**
     * Tenant-scoped lookup by bill UUID.
     */
    @Query("SELECT b FROM Bill b WHERE b.id = :id AND b.tenantId = :tenantId")
    Optional<Bill> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    // -------------------------------------------------------------------------
    // Analytics queries
    // -------------------------------------------------------------------------

    /**
     * Sum of grand totals for bills created within a time window.
     */
    @Query("""
            SELECT COALESCE(SUM(b.grandTotal), 0)
            FROM Bill b
            WHERE b.tenantId = :tenantId
              AND b.status <> com.auradev.erp.billing.entity.BillStatus.VOID
              AND b.createdAt >= :from
              AND b.createdAt < :to
            """)
    BigDecimal sumGrandTotalByTenantIdAndCreatedAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /**
     * Count of bills created within a time window.
     */
    @Query("""
            SELECT COUNT(b)
            FROM Bill b
            WHERE b.tenantId = :tenantId
              AND b.status <> com.auradev.erp.billing.entity.BillStatus.VOID
              AND b.createdAt >= :from
              AND b.createdAt < :to
            """)
    long countByTenantIdAndCreatedAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /**
     * Sum of item quantities sold within a time window (returned as BigDecimal; callers cast as needed).
     */
    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM BillItem i
            WHERE i.bill.tenantId = :tenantId
              AND i.bill.status <> com.auradev.erp.billing.entity.BillStatus.VOID
              AND i.bill.createdAt >= :from
              AND i.bill.createdAt < :to
            """)
    BigDecimal sumItemsQtyByTenantIdAndCreatedAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /**
     * Hourly sales trend for today compared to the same hours yesterday.
     * Returns one point per hour of the current day (0-23).
     */
    @Query(value = """
            WITH hours AS (
                SELECT generate_series(0, 23) AS hr
            ),
            today AS (
                SELECT date_part('hour', created_at AT TIME ZONE 'Asia/Kolkata') AS hr,
                       COALESCE(SUM(grand_total), 0) AS total
                FROM bills
                WHERE tenant_id = :tenantId
                  AND status <> 'VOID'
                  AND created_at >= date_trunc('day', now() AT TIME ZONE 'Asia/Kolkata') AT TIME ZONE 'Asia/Kolkata'
                  AND created_at <  date_trunc('day', now() AT TIME ZONE 'Asia/Kolkata') AT TIME ZONE 'Asia/Kolkata' + interval '1 day'
                GROUP BY 1
            ),
            yesterday AS (
                SELECT date_part('hour', created_at AT TIME ZONE 'Asia/Kolkata') AS hr,
                       COALESCE(SUM(grand_total), 0) AS total
                FROM bills
                WHERE tenant_id = :tenantId
                  AND status <> 'VOID'
                  AND created_at >= date_trunc('day', now() AT TIME ZONE 'Asia/Kolkata') AT TIME ZONE 'Asia/Kolkata' - interval '1 day'
                  AND created_at <  date_trunc('day', now() AT TIME ZONE 'Asia/Kolkata') AT TIME ZONE 'Asia/Kolkata'
                GROUP BY 1
            )
            SELECT to_char(h.hr, 'FM00') || ':00'   AS label,
                   COALESCE(t.total, 0)             AS current,
                   COALESCE(y.total, 0)             AS previous
            FROM hours h
            LEFT JOIN today     t ON t.hr = h.hr
            LEFT JOIN yesterday y ON y.hr = h.hr
            ORDER BY h.hr
            """, nativeQuery = true)
    List<SalesTrendProjection> salesTrendHourly(@Param("tenantId") UUID tenantId);

    /**
     * Daily sales trend for the last {@code days} days vs the preceding equal period.
     */
    @Query(value = """
            WITH dates AS (
                SELECT generate_series(
                    (current_date - (:days - 1) * interval '1 day')::date,
                    current_date,
                    interval '1 day'
                )::date AS d
            ),
            current_period AS (
                SELECT (created_at AT TIME ZONE 'Asia/Kolkata')::date AS d,
                       COALESCE(SUM(grand_total), 0)                  AS total
                FROM bills
                WHERE tenant_id = :tenantId
                  AND status <> 'VOID'
                  AND (created_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN
                      current_date - (:days - 1) AND current_date
                GROUP BY 1
            ),
            prev_period AS (
                SELECT (created_at AT TIME ZONE 'Asia/Kolkata')::date + (:days * interval '1 day')::integer AS d,
                       COALESCE(SUM(grand_total), 0)                                                        AS total
                FROM bills
                WHERE tenant_id = :tenantId
                  AND status <> 'VOID'
                  AND (created_at AT TIME ZONE 'Asia/Kolkata')::date BETWEEN
                      current_date - (2 * :days - 1) AND current_date - :days
                GROUP BY 1
            )
            SELECT to_char(dates.d, 'DD Mon') AS label,
                   COALESCE(c.total, 0)       AS current,
                   COALESCE(p.total, 0)       AS previous
            FROM dates
            LEFT JOIN current_period c ON c.d = dates.d
            LEFT JOIN prev_period    p ON p.d = dates.d
            ORDER BY dates.d
            """, nativeQuery = true)
    List<SalesTrendProjection> salesTrendDaily(@Param("tenantId") UUID tenantId, @Param("days") int days);

    /**
     * Top products by revenue or quantity for a given date range.
     */
    @Query(value = """
            SELECT p.name                           AS name,
                   CAST(SUM(bi.quantity) AS BIGINT) AS qty,
                   SUM(bi.line_total)               AS revenue
            FROM bill_items bi
            JOIN products   p  ON p.id    = bi.product_id
            JOIN bills      b  ON b.id    = bi.bill_id
            WHERE b.tenant_id = :tenantId
              AND b.status <> 'VOID'
              AND (b.created_at AT TIME ZONE 'Asia/Kolkata')::date >= :from
              AND (b.created_at AT TIME ZONE 'Asia/Kolkata')::date <  :to
            GROUP BY p.id, p.name
            ORDER BY (CASE WHEN :byRevenue THEN SUM(bi.line_total) ELSE SUM(bi.quantity) END) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopProductProjection> topProducts(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("byRevenue") boolean byRevenue,
            @Param("limit") int limit);

    /**
     * Flexible aggregated report — groups bills by the requested dimension.
     */
    @Query(value = """
            SELECT
              CASE :groupBy
                WHEN 'day'            THEN to_char((b.created_at AT TIME ZONE 'Asia/Kolkata')::date, 'YYYY-MM-DD')
                WHEN 'category'       THEN COALESCE(cat.name, 'Uncategorised')
                WHEN 'product'        THEN p.name
                WHEN 'payment_method' THEN pay.method
                WHEN 'cashier'        THEN u.name
                ELSE to_char((b.created_at AT TIME ZONE 'Asia/Kolkata')::date, 'YYYY-MM-DD')
              END AS dimension,
              CASE :metric
                WHEN 'bills'  THEN CAST(COUNT(DISTINCT b.id) AS NUMERIC)
                WHEN 'items'  THEN COALESCE(SUM(bi.quantity), 0)
                ELSE               COALESCE(SUM(b.grand_total), 0)
              END AS value,
              CASE :groupBy
                WHEN 'category' THEN COALESCE(cat.name, 'Uncategorised')
                WHEN 'cashier'  THEN u.name
                ELSE NULL
              END AS label
            FROM bills b
            LEFT JOIN bill_items bi    ON bi.bill_id    = b.id
            LEFT JOIN products   p     ON p.id          = bi.product_id
            LEFT JOIN categories cat   ON cat.id        = p.category_id
            LEFT JOIN payments   pay   ON pay.bill_id   = b.id
            LEFT JOIN users      u     ON u.id          = b.cashier_id
            WHERE b.tenant_id = :tenantId
              AND b.status <> 'VOID'
              AND (b.created_at AT TIME ZONE 'Asia/Kolkata')::date >= :from
              AND (b.created_at AT TIME ZONE 'Asia/Kolkata')::date <  :to
            GROUP BY 1, 3
            ORDER BY 2 DESC
            """, nativeQuery = true)
    List<ReportPointProjection> flexibleReport(
            @Param("tenantId") UUID tenantId,
            @Param("metric") String metric,
            @Param("groupBy") String groupBy,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Recent bill activity items for the activity feed.
     */
    @Query(value = """
            SELECT u.name     AS who,
                   'sold'     AS action,
                   b.bill_no  AS detail,
                   b.created_at AS time,
                   'Bill'     AS entityType
            FROM bills b
            JOIN users u ON u.id = b.cashier_id
            WHERE b.tenant_id = :tenantId
              AND b.status <> 'VOID'
            ORDER BY b.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ActivityProjection> recentBillActivity(@Param("tenantId") UUID tenantId, @Param("limit") int limit);
}
