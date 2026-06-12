package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.CustomerType;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String phone,
        CustomerType type,
        int loyaltyPoints,
        BigDecimal creditBalance
) {}
