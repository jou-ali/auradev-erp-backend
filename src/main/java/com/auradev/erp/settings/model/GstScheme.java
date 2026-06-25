package com.auradev.erp.settings.model;

/**
 * How GST is applied on sales bills.
 * <ul>
 *   <li>{@link #PRODUCT} — each line uses the product's {@code tax_rate_pct}</li>
 *   <li>{@link #COMPOSITE} — one rate applied to every line (retail composition)</li>
 *   <li>{@link #CATEGORY} — rate from tenant category mapping</li>
 * </ul>
 */
public enum GstScheme {
    PRODUCT,
    COMPOSITE,
    CATEGORY
}
