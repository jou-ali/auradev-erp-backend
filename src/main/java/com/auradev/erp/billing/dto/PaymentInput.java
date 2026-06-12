package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentInput(
        @NotNull PaymentMethod method,
        BigDecimal tendered,
        BigDecimal splitCashAmount
) {}
