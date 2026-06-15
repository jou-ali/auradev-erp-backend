package com.auradev.erp.importing.inventory;

import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.importing.inventory.dto.ImportRowIssue;
import com.auradev.erp.importing.inventory.dto.InventoryImportResult;
import com.auradev.erp.importing.inventory.dto.StockAdjustmentImportRow;
import com.auradev.erp.importing.spreadsheet.SpreadsheetTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAdjustmentImportService {

    private final StockAdjustmentRowImportExecutor rowImportExecutor;

    public InventoryImportResult importTable(SpreadsheetTable table) {
        if (table.rows().isEmpty()) {
            throw new BusinessException("NO_ROWS", "Spreadsheet has no data rows");
        }

        validateHeaders(table.headers());

        List<StockAdjustmentImportRow> parsed = table.rows().stream()
                .map(StockAdjustmentImportRowMapper::map)
                .toList();

        StockAdjustmentImportValidator.ValidationResult validation =
                StockAdjustmentImportValidator.validate(parsed);

        int stockAdjusted = 0;
        int skipped = 0;
        List<ImportRowIssue> issues = new ArrayList<>(validation.invalidIssues());

        for (StockAdjustmentImportRow row : validation.validRows()) {
            try {
                StockAdjustmentRowImporter.ImportAttempt attempt = rowImportExecutor.importOne(row);
                if (attempt.outcome() == StockAdjustmentRowImporter.Outcome.ADJUSTED) {
                    stockAdjusted++;
                } else {
                    skipped++;
                    issues.add(new ImportRowIssue(
                            row.rowNumber(), row.sku(),
                            "SKU not found in catalogue"));
                }
            } catch (BusinessException e) {
                skipped++;
                issues.add(new ImportRowIssue(row.rowNumber(), row.sku(), e.getMessage()));
                log.warn("Stock import row {} sku={}: {}", row.rowNumber(), row.sku(), e.getMessage());
            }
        }

        log.info("Stock adjustment import total={} adjusted={} skipped={} invalid={}",
                parsed.size(), stockAdjusted, skipped, validation.invalidIssues().size());

        return new InventoryImportResult(
                parsed.size(),
                0,
                stockAdjusted,
                skipped,
                validation.invalidIssues().size(),
                0,
                issues);
    }

    private void validateHeaders(List<String> headers) {
        if (!StockAdjustmentImportSchema.matches(headers)) {
            throw new BusinessException(
                    "INVALID_TEMPLATE",
                    "Stock adjustment sheet requires columns: sku, adjustment, quantity, reason, notes");
        }
    }
}
