package com.auradev.erp.importing.procurement;

import java.util.List;

public final class PurchaseImportSchema {

    public static final String SHEET_NAME = "Purchases";

    /** Line sheet for one supplier — supplier is chosen in the app (like Record purchase). */
    public static final List<String> HEADERS = List.of(
            "purchase_ref",
            "bill_date",
            "due_date",
            "notes",
            "sku",
            "quantity",
            "rate"
    );

    private PurchaseImportSchema() {}
}
