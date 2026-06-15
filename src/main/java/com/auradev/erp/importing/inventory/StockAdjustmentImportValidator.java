package com.auradev.erp.importing.inventory;

import com.auradev.erp.importing.inventory.dto.ImportRowIssue;
import com.auradev.erp.importing.inventory.dto.StockAdjustmentImportRow;

import java.math.BigDecimal;
import java.util.*;

public final class StockAdjustmentImportValidator {

    private StockAdjustmentImportValidator() {}

    public static ValidationResult validate(List<StockAdjustmentImportRow> rows) {
        Set<String> seenSku = new HashSet<>();
        List<StockAdjustmentImportRow> valid = new ArrayList<>();
        List<ImportRowIssue> issues = new ArrayList<>();

        for (StockAdjustmentImportRow row : rows) {
            Optional<String> error = validateRow(row, seenSku);
            if (error.isPresent()) {
                issues.add(new ImportRowIssue(row.rowNumber(), row.sku(), error.get()));
            } else {
                valid.add(row);
                seenSku.add(normalizeKey(row.sku()));
            }
        }

        return new ValidationResult(valid, issues);
    }

    private static Optional<String> validateRow(StockAdjustmentImportRow row, Set<String> seenSku) {
        if (row.sku() == null || row.sku().isBlank()) {
            return Optional.of("SKU is required");
        }
        String skuKey = normalizeKey(row.sku());
        if (seenSku.contains(skuKey)) {
            return Optional.of("Duplicate SKU in file: " + row.sku());
        }
        if (!StockAdjustmentMovementResolver.isValidAdjustment(row.adjustment())) {
            return Optional.of("adjustment must be add or remove");
        }
        if (row.quantity() == null || row.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("quantity must be greater than 0");
        }
        if (!StockAdjustmentMovementResolver.isValidReason(row.reason())) {
            return Optional.of("reason must be grn, damage, return, or count");
        }
        return Optional.empty();
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record ValidationResult(
            List<StockAdjustmentImportRow> validRows,
            List<ImportRowIssue> invalidIssues
    ) {}
}
