package com.auradev.erp.settings.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * GST / tax behaviour stored in {@code tenant_settings.tax} JSONB.
 */
public record TaxConfig(
        GstScheme scheme,
        boolean priceIncludesTax,
        List<Integer> enabledRates,
        BigDecimal compositeRatePct,
        BigDecimal defaultCategoryRatePct,
        List<CategoryGstRate> categoryRates
) {
    public static TaxConfig defaults() {
        return new TaxConfig(
                GstScheme.PRODUCT,
                false,
                List.of(0, 5, 12, 18),
                new BigDecimal("5"),
                new BigDecimal("5"),
                List.of());
    }

    public TaxConfig normalized() {
        GstScheme s = scheme != null ? scheme : GstScheme.PRODUCT;
        List<Integer> rates = normalizeRates(enabledRates);
        BigDecimal composite = clampRate(compositeRatePct != null ? compositeRatePct : new BigDecimal("5"));
        BigDecimal categoryDefault = clampRate(
                defaultCategoryRatePct != null ? defaultCategoryRatePct : new BigDecimal("5"));
        List<CategoryGstRate> cats = categoryRates != null ? categoryRates : List.of();
        List<CategoryGstRate> normalizedCats = new ArrayList<>();
        for (CategoryGstRate row : cats) {
            if (row == null || row.categoryId() == null) {
                continue;
            }
            normalizedCats.add(new CategoryGstRate(row.categoryId(), clampRate(row.ratePct())));
        }
        return new TaxConfig(s, priceIncludesTax, rates, composite, categoryDefault, List.copyOf(normalizedCats));
    }

    private static List<Integer> normalizeRates(List<Integer> rates) {
        if (rates == null || rates.isEmpty()) {
            return List.of(0, 5, 12, 18);
        }
        return rates.stream()
                .filter(r -> r != null)
                .map(r -> Math.max(0, Math.min(100, r)))
                .distinct()
                .sorted()
                .toList();
    }

    private static BigDecimal clampRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal clamped = rate.max(BigDecimal.ZERO).min(new BigDecimal("100"));
        return clamped.stripTrailingZeros();
    }
}
