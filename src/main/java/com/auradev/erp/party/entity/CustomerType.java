package com.auradev.erp.party.entity;

/**
 * Classification of a customer by their shopping behaviour or registration type.
 *
 * <ul>
 *   <li>{@link #WALKIN} — anonymous or one-time walk-in customer.</li>
 *   <li>{@link #B2C} — registered individual / retail consumer.</li>
 *   <li>{@link #B2B} — registered business (may carry a GSTIN).</li>
 * </ul>
 */
public enum CustomerType {
    /** Anonymous walk-in customer — no loyalty account. */
    WALKIN,
    /** Registered retail consumer (B2C). */
    B2C,
    /** Registered business customer (B2B). */
    B2B
}
