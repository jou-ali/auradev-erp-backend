package com.auradev.erp.importing.inventory;

import com.auradev.erp.importing.inventory.dto.StockAdjustmentImportRow;

import java.math.BigDecimal;
import java.util.Map;

public final class StockAdjustmentImportRowMapper {

    private StockAdjustmentImportRowMapper() {}

    public static StockAdjustmentImportRow map(Map<String, String> cells) {
        int row = parseInt(cells.getOrDefault("_row", "0"), 0);
        return new StockAdjustmentImportRow(
                row,
                trim(cells.get("sku")),
                trim(cells.get("adjustment")),
                parseDecimal(cells.get("quantity")),
                trim(cells.get("reason")),
                trim(cells.get("notes")));
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return new BigDecimal(raw.trim().replace(",", ""));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
