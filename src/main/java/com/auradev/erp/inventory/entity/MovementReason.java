package com.auradev.erp.inventory.entity;

/**
 * Business reason that caused a stock movement.
 *
 * <p>Stored as a STRING column on {@link StockMovement} so that new reasons
 * can be added without a DDL ordinal migration.</p>
 */
public enum MovementReason {

    /** Stock consumed by a completed customer sale (POS bill). */
    SALE,

    /** Stock returned by a customer via a credit note or exchange. */
    RETURN,

    /** Goods Received Note — stock received from a supplier purchase order. */
    GRN,

    /** Stock written off due to breakage, expiry, or spoilage. */
    DAMAGE,

    /** Stock level adjusted after a physical count reconciliation. */
    COUNT_CORRECTION,

    /** Free-form manual adjustment authorised by management. */
    MANUAL_ADJUST
}
