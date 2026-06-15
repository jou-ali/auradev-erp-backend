package com.auradev.erp.procurement.repository;

import com.auradev.erp.procurement.entity.PurchaseSequence;
import com.auradev.erp.procurement.entity.PurchaseSequenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseSequenceRepository extends JpaRepository<PurchaseSequence, PurchaseSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PurchaseSequence s WHERE s.id.tenantId = :tenantId AND s.id.fy = :fy")
    Optional<PurchaseSequence> findForUpdate(@Param("tenantId") UUID tenantId, @Param("fy") String fy);
}
