package com.auradev.erp.importing.inventory;

import com.auradev.erp.catalog.entity.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Per-upload state to avoid duplicate creates within a single spreadsheet. */
public class ImportBatchContext {

    private final Set<String> skus = new HashSet<>();
    private final Set<String> barcodes = new HashSet<>();
    private final Map<String, Category> categoriesByName = new HashMap<>();

    public boolean seenSku(String sku) {
        return skus.contains(normalize(sku));
    }

    public void markSku(String sku) {
        skus.add(normalize(sku));
    }

    public boolean seenBarcode(String barcode) {
        return barcode != null && !barcode.isBlank() && barcodes.contains(normalize(barcode));
    }

    public void markBarcode(String barcode) {
        if (barcode != null && !barcode.isBlank()) {
            barcodes.add(normalize(barcode));
        }
    }

    public Category cachedCategory(String name) {
        return categoriesByName.get(normalize(name));
    }

    public void cacheCategory(String name, Category category) {
        categoriesByName.put(normalize(name), category);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
