package com.auradev.erp.importing.inventory;

import com.auradev.erp.importing.inventory.dto.InventoryImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inventory/import")
@Tag(name = "Inventory Import", description = "Bulk product upload from Excel")
@RequiredArgsConstructor
public class InventoryImportController {

    private final InventoryImportService importService;

    @GetMapping("/template")
    @Operation(summary = "Download Excel template for bulk inventory import")
    public ResponseEntity<byte[]> template() {
        byte[] bytes = importService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/sample")
    @Operation(summary = "Download sample Excel with 100 products for bulk import testing")
    public ResponseEntity<byte[]> sample() {
        byte[] bytes = importService.downloadSample();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-import-sample-100.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/stock-sample")
    @Operation(summary = "Download sample Excel for bulk stock adjustments on existing SKUs")
    public ResponseEntity<byte[]> stockSample() {
        byte[] bytes = importService.downloadStockAdjustmentSample();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-stock-adjustment-sample.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload Excel file to import products (skips duplicates)")
    public ResponseEntity<InventoryImportResult> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importFile(file));
    }
}
