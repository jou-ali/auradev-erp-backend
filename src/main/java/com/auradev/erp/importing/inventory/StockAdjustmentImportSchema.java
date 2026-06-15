package com.auradev.erp.importing.inventory;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Columns for bulk stock adjustments — mirrors Quick adjust in the UI. */
public final class StockAdjustmentImportSchema {

    public static final String SHEET_NAME = "Stock adjustments";

    public static final List<String> HEADERS = List.of(
            "sku",
            "adjustment",
            "quantity",
            "reason",
            "notes"
    );

    private static final Set<String> REQUIRED_NORMALIZED = Set.of("sku", "adjustment", "quantity", "reason");

    private StockAdjustmentImportSchema() {}

    public static boolean matches(List<String> headers) {
        Set<String> normalized = headers.stream()
                .map(StockAdjustmentImportSchema::normalizeHeader)
                .collect(Collectors.toSet());
        return normalized.containsAll(REQUIRED_NORMALIZED);
    }

    static String normalizeHeader(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
