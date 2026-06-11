package com.auradev.erp.analytics.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA closed projection for flexible-report native queries.
 * Column aliases in the SQL must match these getter names (case-insensitive).
 */
public interface ReportPointProjection {
    String getDimension();
    BigDecimal getValue();
    String getLabel();
}
