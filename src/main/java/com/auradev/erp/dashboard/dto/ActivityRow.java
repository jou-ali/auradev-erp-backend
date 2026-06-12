package com.auradev.erp.dashboard.dto;

import java.time.Instant;

public record ActivityRow(
        String who,
        String action,
        String detail,
        String icon,
        String tone,
        Instant createdAt
) {}
