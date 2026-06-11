package com.auradev.erp.inventory.entity;

/**
 * Identifies the business document type that caused a stock movement.
 *
 * <p>Together with {@link StockMovement#getReferenceId()} this forms a
 * polymorphic foreign key that links a movement back to its originating
 * document (bill, purchase order, etc.) without requiring nullable columns
 * for every document type.</p>
 */
public enum MovementRefType {

    /** Movement originated from a customer-facing sales bill. */
    BILL,

    /** Movement originated from a supplier purchase / GRN. */
    PURCHASE,

    /** Movement originated from a manual stock adjustment. */
    ADJUSTMENT,

    /** Movement originated from a customer credit note / return. */
    CREDIT_NOTE
}
