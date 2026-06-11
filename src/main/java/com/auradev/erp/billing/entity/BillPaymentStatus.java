package com.auradev.erp.billing.entity;

/**
 * Payment settlement status for a sales bill.
 */
public enum BillPaymentStatus {
    /** Fully paid at the time of sale. */
    PAID,
    /** Partially paid; remaining on credit. */
    PARTIAL,
    /** Entirely on credit (post-paid). */
    CREDIT,
    /** Bill has been voided. */
    VOID
}
