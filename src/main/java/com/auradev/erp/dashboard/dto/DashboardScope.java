package com.auradev.erp.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

/** Resolved time window and optional bill dimensions for dashboard analytics. */
public record DashboardScope(
        String preset,
        Instant periodFrom,
        Instant periodTo,
        Instant compareFrom,
        Instant compareTo,
        UUID customerId,
        UUID productId,
        String periodLabel,
        String compareLabel,
        int trendPointCount
) {}
