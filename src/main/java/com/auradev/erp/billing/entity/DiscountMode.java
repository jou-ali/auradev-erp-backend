package com.auradev.erp.billing.entity;

/**
 * How the bill-level discount is expressed.
 */
public enum DiscountMode {
    /** The discount value is an absolute monetary amount. */
    AMOUNT,
    /** The discount value is a percentage of the subtotal. */
    PERCENT
}
