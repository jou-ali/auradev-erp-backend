package com.auradev.erp.importing.supplier;

import com.auradev.erp.catalog.dto.CreateSupplierRequest;
import com.auradev.erp.catalog.service.CatalogService;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.importing.inventory.dto.ImportRowIssue;
import com.auradev.erp.importing.spreadsheet.SpreadsheetReaderRegistry;
import com.auradev.erp.importing.spreadsheet.SpreadsheetTable;
import com.auradev.erp.importing.supplier.dto.SupplierImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierImportService {

    private final SpreadsheetReaderRegistry spreadsheetReaders;
    private final SupplierImportTemplateGenerator templateGenerator;
    private final CatalogService catalogService;

    public byte[] downloadTemplate() {
        try {
            return templateGenerator.generate();
        } catch (IOException e) {
            throw new BusinessException("TEMPLATE_FAILED", "Could not generate supplier import template");
        }
    }

    public SupplierImportResult importFile(MultipartFile file) {
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

        if (table.rows().isEmpty()) {
            throw new BusinessException("NO_ROWS", "Spreadsheet has no data rows");
        }

        validateHeaders(table.headers());

        int imported = 0;
        int skippedDuplicates = 0;
        int skippedInvalid = 0;
        List<ImportRowIssue> issues = new ArrayList<>();

        for (Map<String, String> cells : table.rows()) {
            int row = parseRow(cells);
            String name = trim(cells.get("name"));
            if (name.isBlank()) {
                skippedInvalid++;
                issues.add(new ImportRowIssue(row, "", "Supplier name is required"));
                continue;
            }

            try {
                catalogService.createSupplier(new CreateSupplierRequest(
                        name,
                        blankToNull(cells.get("contact_person")),
                        blankToNull(cells.get("phone")),
                        blankToNull(cells.get("email")),
                        blankToNull(cells.get("gstin")),
                        blankToNull(cells.get("address"))));
                imported++;
            } catch (BusinessException e) {
                if ("DUPLICATE_SUPPLIER".equals(e.getCode()) || "DUPLICATE_GSTIN".equals(e.getCode())) {
                    skippedDuplicates++;
                    issues.add(new ImportRowIssue(row, name, e.getMessage()));
                } else {
                    skippedInvalid++;
                    issues.add(new ImportRowIssue(row, name, e.getMessage()));
                }
            }
        }

        return new SupplierImportResult(
                table.rows().size(),
                imported,
                skippedDuplicates,
                skippedInvalid,
                issues);
    }

    private void validateHeaders(List<String> headers) {
        if (!headers.contains("name")) {
            throw new BusinessException("INVALID_HEADERS",
                    "Missing required column: name. Download the template and keep header names unchanged.");
        }
    }

    private static int parseRow(Map<String, String> cells) {
        try {
            return Integer.parseInt(cells.getOrDefault("_row", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private static String blankToNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }
}
