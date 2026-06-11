package com.auradev.erp.billing.entity;

/**
 * Tender methods accepted at the point of sale.
 *
 * <p>{@code SPLIT_COMPONENT} is used internally when a bill is paid with multiple
 * tender types; each individual component is stored as a separate {@link Payment}
 * row with this method.</p>
 */
public enum PaymentMethod {
    CASH,
    UPI,
    CARD,
    CREDIT,
    SPLIT_COMPONENT
}
