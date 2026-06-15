package com.auradev.erp.importing.procurement;

import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.entity.Supplier;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.catalog.repository.SupplierRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.importing.inventory.dto.ImportRowIssue;
import com.auradev.erp.importing.procurement.dto.PurchaseImportResult;
import com.auradev.erp.importing.spreadsheet.SpreadsheetReaderRegistry;
import com.auradev.erp.importing.spreadsheet.SpreadsheetTable;
import com.auradev.erp.procurement.dto.CreatePurchaseRequest;
import com.auradev.erp.procurement.dto.PurchaseLineRequest;
import com.auradev.erp.procurement.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseImportService {

    private final SpreadsheetReaderRegistry spreadsheetReaders;
    private final PurchaseImportTemplateGenerator templateGenerator;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseService purchaseService;

    public byte[] downloadTemplate() {
        try {
            return templateGenerator.generate();
        } catch (IOException e) {
            throw new BusinessException("TEMPLATE_FAILED", "Could not generate purchase import template");
        }
    }

    public PurchaseImportResult importFile(MultipartFile file, UUID supplierId) {
        if (supplierId == null) {
            throw new BusinessException("SUPPLIER_REQUIRED", "Select a supplier before uploading");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(Supplier::isActive)
                .orElseThrow(() -> new BusinessException("UNKNOWN_SUPPLIER", "Supplier not found"));

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

        List<PurchaseImportRow> parsed = table.rows().stream()
                .map(PurchaseImportRowMapper::map)
                .toList();

        List<ImportRowIssue> issues = new ArrayList<>();
        List<PurchaseImportRow> valid = new ArrayList<>();
        int skippedInvalid = 0;

        for (PurchaseImportRow row : parsed) {
            String err = validateRow(row);
            if (err != null) {
                skippedInvalid++;
                issues.add(new ImportRowIssue(row.rowNumber(), row.sku(), err));
            } else {
                valid.add(row);
            }
        }

        Map<String, List<PurchaseImportRow>> groups = new LinkedHashMap<>();
        for (PurchaseImportRow row : valid) {
            groups.computeIfAbsent(groupKey(row), k -> new ArrayList<>()).add(row);
        }

        int purchasesCreated = 0;

        for (List<PurchaseImportRow> group : groups.values()) {
            PurchaseImportRow head = group.getFirst();
            String groupErr = validateGroup(group);
            if (groupErr != null) {
                for (PurchaseImportRow line : group) {
                    skippedInvalid++;
                    issues.add(new ImportRowIssue(line.rowNumber(), line.sku(), groupErr));
                }
                continue;
            }

            try {
                List<PurchaseLineRequest> items = new ArrayList<>();
                for (PurchaseImportRow line : group) {
                    Product product = productRepository.findBySkuIgnoreCase(line.sku().trim())
                            .orElseThrow(() -> new BusinessException("UNKNOWN_SKU",
                                    "SKU not found in inventory: " + line.sku()));
                    items.add(new PurchaseLineRequest(
                            product.getId(),
                            line.quantity(),
                            line.rate(),
                            null));
                }

                purchaseService.create(new CreatePurchaseRequest(
                        supplier.getId(),
                        head.billDate(),
                        head.dueDate(),
                        blankToNull(head.notes()),
                        items));
                purchasesCreated++;
            } catch (BusinessException e) {
                for (PurchaseImportRow line : group) {
                    skippedInvalid++;
                    issues.add(new ImportRowIssue(line.rowNumber(), line.sku(), e.getMessage()));
                }
            }
        }

        return new PurchaseImportResult(
                parsed.size(),
                purchasesCreated,
                0,
                skippedInvalid,
                issues);
    }

    private static String groupKey(PurchaseImportRow row) {
        if (row.purchaseRef() != null && !row.purchaseRef().isBlank()) {
            return "ref:" + row.purchaseRef().trim().toLowerCase(Locale.ROOT);
        }
        return "date:" + row.billDate();
    }

    private static String validateGroup(List<PurchaseImportRow> group) {
        PurchaseImportRow head = group.getFirst();
        for (PurchaseImportRow row : group) {
            if (!Objects.equals(head.billDate(), row.billDate())) {
                return "All lines in a purchase_ref must share the same bill_date";
            }
            if (!Objects.equals(head.dueDate(), row.dueDate())) {
                return "All lines in a purchase_ref must share the same due_date";
            }
        }
        return null;
    }

    private String validateRow(PurchaseImportRow row) {
        if (row.billDate() == null) {
            return "bill_date is required (YYYY-MM-DD)";
        }
        if (row.sku() == null || row.sku().isBlank()) {
            return "sku is required";
        }
        String sku = row.sku().trim();
        if (productRepository.findBySkuIgnoreCase(sku).isEmpty()) {
            return "SKU not in inventory: " + sku + " — copy exact SKU from Inventory screen";
        }
        if (row.quantity() == null || row.quantity().signum() <= 0) {
            return "quantity must be greater than zero";
        }
        if (row.rate() == null || row.rate().signum() <= 0) {
            return "rate must be greater than zero";
        }
        return null;
    }

    private void validateHeaders(List<String> headers) {
        for (String required : List.of("bill_date", "sku", "quantity", "rate")) {
            if (!headers.contains(required)) {
                throw new BusinessException("INVALID_HEADERS",
                        "Missing required column: " + required + ". Download the template.");
            }
        }
    }

    private static String blankToNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }
}
