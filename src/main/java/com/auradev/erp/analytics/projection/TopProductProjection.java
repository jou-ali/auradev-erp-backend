package com.auradev.erp.analytics.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA closed projection for top-products native queries.
 * Column aliases in the SQL must match these getter names (case-insensitive).
 */
public interface TopProductProjection {
    String getName();
    Long getQty();
    BigDecimal getRevenue();
}
