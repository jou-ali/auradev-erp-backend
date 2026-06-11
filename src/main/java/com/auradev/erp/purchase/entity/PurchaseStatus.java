package com.auradev.erp.purchase.entity;

/**
 * Lifecycle states of a purchase order / bill in the ERP.
 *
 * <p>Allowed transitions:
 * <pre>
 *   DRAFT → PENDING_GRN → BILLED → PAID
 * </pre>
 * </p>
 */
public enum PurchaseStatus {
    /** Initial state; the purchase has been created but goods have not yet arrived. */
    DRAFT,
    /** Goods Receipt Note (GRN) is pending — the order has been confirmed with the supplier. */
    PENDING_GRN,
    /** Goods have been received and the supplier invoice has been recorded. */
    BILLED,
    /** The supplier invoice has been settled (payment issued). */
    PAID
}
