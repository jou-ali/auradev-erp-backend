package com.auradev.erp.analytics.dto;

import java.math.BigDecimal;

/**
 * A single data point in a flexible analytics report.
 *
 * <p>The meaning of each field depends on the {@code groupBy} and
 * {@code metric} parameters supplied to the report endpoint:</p>
 * <ul>
 *   <li>{@code groupBy=day} — {@code dimension} is an ISO date string.</li>
 *   <li>{@code groupBy=category} — {@code dimension} is the category name.</li>
 *   <li>{@code groupBy=product} — {@code dimension} is the product name.</li>
 *   <li>{@code groupBy=payment_method} — {@code dimension} is the payment method.</li>
 *   <li>{@code groupBy=cashier} — {@code dimension} is the cashier's display name.</li>
 * </ul>
 *
 * @param dimension the x-axis or group-by value
 * @param value     the aggregated metric value
 * @param label     optional human-readable label override for the dimension
 */
public record ReportPoint(
        String dimension,
        BigDecimal value,
        String label
) {}
