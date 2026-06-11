package com.auradev.erp.analytics.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA closed projection for sales-trend native queries.
 * Column aliases in the SQL must match these getter names (case-insensitive).
 */
public interface SalesTrendProjection {
    String getLabel();
    BigDecimal getCurrent();
    BigDecimal getPrevious();
}
