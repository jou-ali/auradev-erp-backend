package com.auradev.erp.billing.entity;

/**
 * Payment settlement status for a {@link Bill}.
 *
 * <p>This enum supersedes {@link BillPaymentStatus} and aligns with the
 * canonical API contract.  The {@code Bill} entity field
 * {@code paymentStatus} uses this type.</p>
 *
 * <ul>
 *   <li>{@link #PAID}    — fully settled at the time of sale.</li>
 *   <li>{@link #PARTIAL} — partially paid; the outstanding balance is on credit.</li>
 *   <li>{@link #CREDIT}  — entirely on credit (post-paid / open account).</li>
 *   <li>{@link #VOID}    — bill has been voided; no settlement expected.</li>
 * </ul>
 */
public enum PaymentStatus {
    /** Bill fully paid at time of sale. */
    PAID,
    /** Bill partially paid; remainder on credit. */
    PARTIAL,
    /** Entire bill on credit (open-account / post-paid). */
    CREDIT,
    /** Bill voided — no settlement applicable. */
    VOID
}
