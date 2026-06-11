package com.auradev.erp.billing.entity;

/**
 * Lifecycle states for a sales bill.
 *
 * <p>Allowed transitions:
 * <pre>
 *   COMPLETED ← (created)
 *   COMPLETED → HELD
 *   COMPLETED → VOID
 *   HELD → VOID
 * </pre>
 * </p>
 */
public enum BillStatus {
    /** Bill has been completed and stock deducted. */
    COMPLETED,
    /** Bill is on hold; stock is NOT reversed. */
    HELD,
    /** Bill has been voided; stock movements reversed. */
    VOID
}
