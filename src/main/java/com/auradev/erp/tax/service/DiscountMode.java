package com.auradev.erp.tax.service;

/**
 * Specifies how a bill-level discount should be interpreted by
 * {@link GstCalculatorService}.
 *
 * <ul>
 *   <li>{@link #AMOUNT} — the discount is an absolute monetary value
 *       (e.g. ₹50 off the total).</li>
 *   <li>{@link #PERCENT} — the discount is a percentage of the pre-discount
 *       subtotal (e.g. 10 % off).</li>
 * </ul>
 */
public enum DiscountMode {
    AMOUNT,
    PERCENT
}
