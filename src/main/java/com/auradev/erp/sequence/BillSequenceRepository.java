package com.auradev.erp.sequence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link BillSequence} counters.
 *
 * <p>The {@link #findByTenantIdAndFyForUpdate} method acquires a
 * pessimistic write lock to prevent concurrent allocations for the same
 * tenant + FY from producing duplicate sequence numbers.</p>
 */
@Repository
public interface BillSequenceRepository extends JpaRepository<BillSequence, BillSequence.BillSequenceId> {

    /**
     * Fetch the sequence row for a tenant + FY with a pessimistic write lock.
     *
     * @param tenantId the tenant UUID
     * @param fy       the financial year string (e.g. {@code "2526"})
     * @return the locked sequence row, or empty if none exists yet
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BillSequence s WHERE s.tenantId = :tenantId AND s.fy = :fy")
    Optional<BillSequence> findByTenantIdAndFyForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("fy") String fy);
}
