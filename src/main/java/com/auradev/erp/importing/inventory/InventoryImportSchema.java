package com.auradev.erp.importing.inventory;

import java.util.List;

/** Single source of truth for bulk inventory import columns. */
public final class InventoryImportSchema {

    public static final String SHEET_NAME = "Products";

    public static final List<String> HEADERS = List.of(
            "name",
            "sku",
            "barcode",
            "category",
            "unit_type",
            "unit_label",
            "mrp",
            "selling_price",
            "cost_price",
            "tax_rate_pct",
            "initial_stock",
            "reorder_level"
    );

    private InventoryImportSchema() {}
}
