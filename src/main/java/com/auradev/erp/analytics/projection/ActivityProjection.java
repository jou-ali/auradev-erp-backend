package com.auradev.erp.analytics.projection;

import java.time.Instant;

/**
 * Spring Data JPA closed projection for activity-feed native queries.
 * Column aliases in the SQL must match these getter names (case-insensitive).
 */
public interface ActivityProjection {
    String getWho();
    String getAction();
    String getDetail();
    Instant getTime();
    String getEntityType();
}
