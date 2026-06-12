package com.auradev.erp.billing.repository;

import com.auradev.erp.billing.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

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
}
