package com.auradev.erp.billing.repository;

import com.auradev.erp.billing.dto.BillSummaryResponse;
import com.auradev.erp.billing.entity.Bill;
import com.auradev.erp.billing.entity.BillStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Bill> findByTenantIdAndStatusOrderByUpdatedAtDesc(UUID tenantId, BillStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Bill> findByIdAndTenantIdAndStatus(UUID id, UUID tenantId, BillStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Bill> findByTenantIdAndCustomerIdAndStatus(UUID tenantId, UUID customerId, BillStatus status);

    @Query("""
            SELECT new com.auradev.erp.billing.dto.BillSummaryResponse(
                b.id,
                b.billNo,
                (SELECT c.name FROM Customer c WHERE c.id = b.customerId),
                (SELECT u.name FROM User u WHERE u.id = b.cashierId),
                SIZE(b.items),
                b.grandTotal,
                b.paymentStatus,
                b.createdAt)
            FROM Bill b
            WHERE b.tenantId = :tenantId
              AND b.status = :status
              AND (:q IS NULL OR :q = '' OR lower(b.billNo) LIKE lower(concat('%', :q, '%')))
            ORDER BY b.createdAt DESC
            """)
    Page<BillSummaryResponse> searchCompletedSummaries(
            @Param("tenantId") UUID tenantId,
            @Param("status") BillStatus status,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT bi.product.id, COALESCE(SUM(bi.quantity), 0)
            FROM BillItem bi
            JOIN bi.bill b
            WHERE b.tenantId = :tenantId
              AND b.status = :heldStatus
              AND (:excludeBillId IS NULL OR b.id <> :excludeBillId)
            GROUP BY bi.product.id
            """)
    List<Object[]> heldQuantityRows(
            @Param("tenantId") UUID tenantId,
            @Param("heldStatus") BillStatus heldStatus,
            @Param("excludeBillId") UUID excludeBillId);

    default Map<UUID, BigDecimal> sumHeldQuantitiesByProduct(UUID tenantId, UUID excludeBillId) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : heldQuantityRows(tenantId, BillStatus.HELD, excludeBillId)) {
            map.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    @Query("""
            SELECT b FROM Bill b
            WHERE b.tenantId = :tenantId
              AND lower(b.billNo) LIKE lower(concat('%', :q, '%'))
            ORDER BY b.createdAt DESC
            """)
    Page<Bill> searchByBillNo(
            @Param("tenantId") UUID tenantId,
            @Param("q") String q,
            Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("""
            SELECT b FROM Bill b
            WHERE b.tenantId = :tenantId
              AND b.status = :status
              AND (:q IS NULL OR :q = '' OR lower(b.billNo) LIKE lower(concat('%', :q, '%')))
            ORDER BY b.createdAt DESC
            """)
    List<Bill> findCompletedForExport(
            @Param("tenantId") UUID tenantId,
            @Param("status") BillStatus status,
            @Param("q") String q);
}
