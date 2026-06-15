package com.auradev.erp.importing.supplier;

import java.util.List;

public final class SupplierImportSchema {

    public static final String SHEET_NAME = "Suppliers";

    public static final List<String> HEADERS = List.of(
            "name",
            "contact_person",
            "phone",
            "email",
            "gstin",
            "address"
    );

    private SupplierImportSchema() {}
}
