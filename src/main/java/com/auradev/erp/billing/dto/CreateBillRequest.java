package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.DiscountMode;
import com.auradev.erp.settings.model.GstScheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateBillRequest(
        UUID customerId,
        @NotNull DiscountMode discountMode,
        BigDecimal billDiscount,
        @NotEmpty @Valid List<BillLineRequest> items,
        @NotNull @Valid PaymentInput payment,
        GstScheme gstSchemeOverride
) {}
