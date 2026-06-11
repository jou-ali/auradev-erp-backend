package com.auradev.erp.billing.repository;

import com.auradev.erp.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data-access layer for {@link Payment} records.
 *
 * <p>Payments are child records of a {@link com.auradev.erp.billing.entity.Bill}.
 * This repository is used for direct queries when the bill aggregate is not loaded.</p>
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Return all payment rows for a given bill.
     *
     * @param billId the bill UUID
     * @return the list of payments; empty if the bill had no payments recorded separately
     */
    List<Payment> findByBillId(UUID billId);
}
