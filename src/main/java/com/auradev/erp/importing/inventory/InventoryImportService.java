package com.auradev.erp.importing.inventory;

import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.importing.inventory.dto.ImportRowIssue;
import com.auradev.erp.importing.inventory.dto.InventoryImportResult;
import com.auradev.erp.importing.inventory.dto.InventoryImportRow;
import com.auradev.erp.importing.spreadsheet.SpreadsheetReaderRegistry;
import com.auradev.erp.importing.spreadsheet.SpreadsheetTable;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryImportService {

    private final SpreadsheetReaderRegistry spreadsheetReaders;
    private final InventoryImportTemplateGenerator templateGenerator;
    private final InventoryImportSampleGenerator sampleGenerator;
    private final InventoryStockAdjustmentSampleGenerator stockAdjustmentSampleGenerator;
    private final InventoryRowImportExecutor rowImportExecutor;
    private final StockAdjustmentImportService stockAdjustmentImportService;

    @Transactional(readOnly = true)
    public byte[] downloadSample() {
        try {
            return sampleGenerator.load();
        } catch (IOException e) {
            throw new BusinessException("SAMPLE_FAILED", "Could not load sample import file");
        }
    }

    @Transactional(readOnly = true)
    public byte[] downloadTemplate() {
        try {
            return templateGenerator.generate();
        } catch (IOException e) {
            throw new BusinessException("TEMPLATE_FAILED", "Could not generate import template");
        }
    }

    @Transactional(readOnly = true)
    public byte[] downloadStockAdjustmentSample() {
        try {
            return stockAdjustmentSampleGenerator.generate();
        } catch (IOException e) {
            throw new BusinessException("STOCK_SAMPLE_FAILED", "Could not generate stock adjustment sample");
        }
    }

    public InventoryImportResult importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Upload an Excel file (.xlsx)");
        }

        SpreadsheetTable table;
        try {
            table = spreadsheetReaders.read(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException("READ_FAILED", "Could not read spreadsheet: " + e.getMessage());
        }

        if (StockAdjustmentImportSchema.matches(table.headers())) {
            return stockAdjustmentImportService.importTable(table);
        }

        return importProducts(table);
    }

    private InventoryImportResult importProducts(SpreadsheetTable table) {
        UUID tenantId = TenantContext.require();

        if (table.rows().isEmpty()) {
            throw new BusinessException("NO_ROWS", "Spreadsheet has no data rows");
        }

        validateProductHeaders(table.headers());

        List<InventoryImportRow> parsed = table.rows().stream()
                .map(InventoryImportRowMapper::map)
                .toList();

        InventoryImportValidator.ValidationResult validation =
                InventoryImportValidator.validate(parsed);

        int imported = 0;
        int skippedDuplicates = 0;
        int categoriesCreated = 0;
        List<ImportRowIssue> issues = new ArrayList<>(validation.invalidIssues());
        ImportBatchContext batch = new ImportBatchContext();

        for (InventoryImportRow row : validation.validRows()) {
            try {
                InventoryRowImporter.ImportAttempt attempt =
                        rowImportExecutor.importOne(row, tenantId, batch);
                switch (attempt.outcome()) {
                    case IMPORTED -> {
                        imported++;
                        if (attempt.categoryCreated()) categoriesCreated++;
                    }
                    case DUPLICATE_SKU -> {
                        skippedDuplicates++;
                        issues.add(new ImportRowIssue(
                                row.rowNumber(), row.sku(), "SKU already exists in catalogue"));
                    }
                    case DUPLICATE_BARCODE -> {
                        skippedDuplicates++;
                        issues.add(new ImportRowIssue(
                                row.rowNumber(), row.sku(),
                                "Barcode already exists in catalogue: " + row.barcode()));
                    }
                }
            } catch (DataIntegrityViolationException e) {
                skippedDuplicates++;
                String detail = e.getMostSpecificCause().getMessage();
                issues.add(new ImportRowIssue(
                        row.rowNumber(), row.sku(),
                        "Skipped due to catalogue conflict: " + detail));
                log.warn("Inventory import row {} sku={} conflict: {}",
                        row.rowNumber(), row.sku(), detail);
            }
        }

        log.info("Product import tenant={} total={} imported={} skipped={} invalid={}",
                tenantId, parsed.size(), imported, skippedDuplicates, validation.invalidIssues().size());

        return new InventoryImportResult(
                parsed.size(),
                imported,
                0,
                skippedDuplicates,
                validation.invalidIssues().size(),
                categoriesCreated,
                issues);
    }

    private void validateProductHeaders(List<String> headers) {
        List<String> missing = InventoryImportSchema.HEADERS.stream()
                .filter(required -> headers.stream().noneMatch(h -> h.equalsIgnoreCase(required)))
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(
                    "INVALID_TEMPLATE",
                    "Missing required columns: " + String.join(", ", missing)
                            + ". For stock adjustments use: sku, adjustment, quantity, reason, notes");
        }
    }
}
