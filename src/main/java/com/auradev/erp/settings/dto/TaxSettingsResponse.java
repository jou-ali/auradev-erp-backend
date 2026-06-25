package com.auradev.erp.settings.dto;

import com.auradev.erp.settings.model.GstScheme;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TaxSettingsResponse(
        GstScheme scheme,
        boolean priceIncludesTax,
        List<Integer> enabledRates,
        BigDecimal compositeRatePct,
        BigDecimal defaultCategoryRatePct,
        List<CategoryGstRateResponse> categoryRates
) {
    public record CategoryGstRateResponse(
            UUID categoryId,
            String categoryName,
            BigDecimal ratePct
    ) {}
}
