package com.auradev.erp.settings.dto;

import com.auradev.erp.settings.model.GstScheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateTaxSettingsRequest(
        GstScheme scheme,
        Boolean priceIncludesTax,
        List<@Min(0) @Max(100) Integer> enabledRates,
        @DecimalMin("0") @DecimalMax("100") BigDecimal compositeRatePct,
        @DecimalMin("0") @DecimalMax("100") BigDecimal defaultCategoryRatePct,
        List<@Valid CategoryGstRateRequest> categoryRates
) {
    public record CategoryGstRateRequest(
            @NotNull UUID categoryId,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ratePct
    ) {}
}
