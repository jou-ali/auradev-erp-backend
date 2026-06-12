package com.auradev.erp.billing.repository;

import com.auradev.erp.billing.entity.BillSequence;
import com.auradev.erp.billing.entity.BillSequenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillSequenceRepository extends JpaRepository<BillSequence, BillSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BillSequence s WHERE s.id.tenantId = :tenantId AND s.id.fy = :fy")
    Optional<BillSequence> findForUpdate(@Param("tenantId") java.util.UUID tenantId, @Param("fy") String fy);
}
