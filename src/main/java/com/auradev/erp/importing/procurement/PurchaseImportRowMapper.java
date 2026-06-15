package com.auradev.erp.importing.procurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class PurchaseImportRowMapper {

    private PurchaseImportRowMapper() {}

    public static PurchaseImportRow map(Map<String, String> cells) {
        int row = parseInt(cells.getOrDefault("_row", "0"), 0);
        return new PurchaseImportRow(
                row,
                trim(cells.get("purchase_ref")),
                parseDate(cells.get("bill_date")),
                parseDate(cells.get("due_date")),
                trim(cells.get("notes")),
                trim(cells.get("sku")),
                parseDecimal(cells.get("quantity")),
                parseDecimal(cells.get("rate")));
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
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
