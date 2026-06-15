package com.auradev.erp.importing.inventory;

import com.auradev.erp.catalog.entity.UnitType;
import com.auradev.erp.importing.inventory.dto.InventoryImportRow;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/** Maps normalized spreadsheet cells to a typed import row. */
public final class InventoryImportRowMapper {

    private InventoryImportRowMapper() {}

    public static InventoryImportRow map(Map<String, String> cells) {
        int row = parseInt(cells.getOrDefault("_row", "0"), 0);
        UnitType unitType = parseUnitType(cells.get("unit_type"));
        String unitLabel = blankToDefault(cells.get("unit_label"), defaultLabel(unitType));

        return new InventoryImportRow(
                row,
                trim(cells.get("name")),
                trim(cells.get("sku")),
                trim(cells.get("barcode")),
                trim(cells.get("category")),
                unitType,
                unitLabel,
                parseDecimal(cells.get("mrp")),
                parseDecimal(cells.get("selling_price")),
                parseDecimal(cells.get("cost_price")),
                parseDecimal(cells.get("tax_rate_pct")),
                parseDecimal(cells.get("initial_stock")),
                parseDecimal(cells.get("reorder_level")));
    }

    private static UnitType parseUnitType(String raw) {
        if (raw == null || raw.isBlank()) return UnitType.unit;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "kg", "weight_kg", "weight" -> UnitType.weight_kg;
            case "g", "weight_g" -> UnitType.weight_g;
            case "l", "volume_l" -> UnitType.volume_l;
            case "ml", "volume_ml" -> UnitType.volume_ml;
            default -> UnitType.unit;
        };
    }

    private static String defaultLabel(UnitType unitType) {
        return unitType == UnitType.weight_kg || unitType == UnitType.weight_g ? "kg" : "pcs";
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

    private static String blankToDefault(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
