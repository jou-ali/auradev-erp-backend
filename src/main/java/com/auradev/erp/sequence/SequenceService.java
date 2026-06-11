package com.auradev.erp.sequence;

import java.util.UUID;

/**
 * Generates gapless, ordered sequence numbers for tenant-scoped documents.
 *
 * <p>All methods use pessimistic row-level locking on the sequence table to
 * prevent gaps even under concurrent POS sessions.  Callers must hold an
 * open transaction; the sequence row is updated within the same transaction
 * so that a rollback also reverts the sequence increment.</p>
 */
public interface SequenceService {

    /**
     * Allocate and return the next formatted bill number for the given tenant.
     *
     * <p>The returned string has the form: {@code <prefix>-<FY>-<zero-padded-seq>},
     * e.g. {@code ERP-2526-00042}.  The prefix comes from the tenant's
     * {@code bill_no_prefix} setting.</p>
     *
     * @param tenantId the tenant for which to generate the number
     * @return the formatted bill number, unique within the tenant + FY
     */
    String nextBillNo(UUID tenantId);

    /**
     * Allocate and return the next formatted purchase order number.
     *
     * @param tenantId the tenant for which to generate the number
     * @return the formatted purchase number
     */
    String nextPurchaseNo(UUID tenantId);

    /**
     * Allocate and return the next formatted credit-note number.
     *
     * @param tenantId the tenant for which to generate the number
     * @return the formatted credit note number
     */
    String nextCreditNoteNo(UUID tenantId);
}
