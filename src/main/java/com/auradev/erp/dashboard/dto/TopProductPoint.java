package com.auradev.erp.dashboard.dto;

import java.math.BigDecimal;

public record TopProductPoint(
        String name,
        long quantity,
        BigDecimal revenue
) {}
