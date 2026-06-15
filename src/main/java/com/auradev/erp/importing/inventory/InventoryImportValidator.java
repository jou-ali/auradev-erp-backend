package com.auradev.erp.importing.inventory;

import com.auradev.erp.importing.inventory.dto.ImportRowIssue;
import com.auradev.erp.importing.inventory.dto.InventoryImportRow;

import java.math.BigDecimal;
import java.util.*;

public final class InventoryImportValidator {

    private InventoryImportValidator() {}

    public static ValidationResult validate(List<InventoryImportRow> rows) {
        Set<String> seenSku = new HashSet<>();
        Set<String> seenBarcode = new HashSet<>();
        List<InventoryImportRow> valid = new ArrayList<>();
        List<ImportRowIssue> issues = new ArrayList<>();

        for (InventoryImportRow row : rows) {
            Optional<String> error = validateRow(row, seenSku, seenBarcode);
            if (error.isPresent()) {
                issues.add(new ImportRowIssue(row.rowNumber(), row.sku(), error.get()));
            } else {
                valid.add(row);
                seenSku.add(normalizeKey(row.sku()));
                if (row.barcode() != null && !row.barcode().isBlank()) {
                    seenBarcode.add(normalizeKey(row.barcode()));
                }
            }
        }

        return new ValidationResult(valid, issues);
    }

    private static Optional<String> validateRow(
            InventoryImportRow row, Set<String> seenSku, Set<String> seenBarcode) {
        if (row.name() == null || row.name().isBlank()) {
            return Optional.of("Product name is required");
        }
        if (row.sku() == null || row.sku().isBlank()) {
            return Optional.of("SKU is required");
        }
        if (row.categoryName() == null || row.categoryName().isBlank()) {
            return Optional.of("Category is required");
        }
        if (row.mrp() == null || row.mrp().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("MRP must be greater than 0");
        }
        if (row.sellingPrice() == null || row.sellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("Selling price must be greater than 0");
        }
        if (row.taxRatePct() != null && row.taxRatePct().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Tax rate cannot be negative");
        }
        if (row.initialStock() != null && row.initialStock().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Initial stock cannot be negative");
        }
        if (row.reorderLevel() != null && row.reorderLevel().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("Reorder level cannot be negative");
        }

        String skuKey = normalizeKey(row.sku());
        if (seenSku.contains(skuKey)) {
            return Optional.of("Duplicate SKU in file: " + row.sku());
        }

        if (row.barcode() != null && !row.barcode().isBlank()) {
            String barcodeKey = normalizeKey(row.barcode());
            if (seenBarcode.contains(barcodeKey)) {
                return Optional.of("Duplicate barcode in file: " + row.barcode());
            }
        }

        return Optional.empty();
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record ValidationResult(
            List<InventoryImportRow> validRows,
            List<ImportRowIssue> invalidIssues
    ) {}
}
