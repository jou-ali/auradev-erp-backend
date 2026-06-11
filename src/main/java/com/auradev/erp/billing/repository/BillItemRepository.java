package com.auradev.erp.billing.repository;

import com.auradev.erp.billing.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data-access layer for {@link BillItem} line-item entities.
 *
 * <p>Bill items are child records owned by {@link com.auradev.erp.billing.entity.Bill};
 * they are typically accessed via the Bill's cascade operations.  This repository
 * exists for read-only queries that need to traverse items independently of the
 * parent (e.g. analytics, void reversal).</p>
 */
@Repository
public interface BillItemRepository extends JpaRepository<BillItem, UUID> {

    /**
     * Return all line items belonging to the given bill, ordered by insertion.
     *
     * @param billId the bill UUID
     * @return the list of bill items, empty if none exist
     */
    List<BillItem> findByBillId(UUID billId);
}
