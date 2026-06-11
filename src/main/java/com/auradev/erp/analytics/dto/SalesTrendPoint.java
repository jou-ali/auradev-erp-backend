package com.auradev.erp.analytics.dto;

import java.math.BigDecimal;

/**
 * A single data point in a sales-trend series.
 *
 * <p>Used to render a comparison chart where the current period is overlaid
 * against the equivalent previous period.</p>
 *
 * @param label    human-readable x-axis label (e.g. "Mon", "Week 1", "Jan")
 * @param current  sales value for the current period at this label
 * @param previous sales value for the equivalent previous period at this label
 */
public record SalesTrendPoint(
        String label,
        BigDecimal current,
        BigDecimal previous
) {}
