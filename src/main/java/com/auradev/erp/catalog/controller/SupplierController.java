package com.auradev.erp.catalog.controller;

import com.auradev.erp.catalog.dto.CreateSupplierRequest;
import com.auradev.erp.catalog.dto.SupplierResponse;
import com.auradev.erp.catalog.service.CatalogService;
import com.auradev.erp.importing.supplier.SupplierImportService;
import com.auradev.erp.importing.supplier.dto.SupplierImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Supplier directory for procurement")
@RequiredArgsConstructor
public class SupplierController {

    private final CatalogService catalogService;
    private final SupplierImportService supplierImportService;

    @GetMapping
    @PreAuthorize("@authz.canAny(authentication, 'PURCHASE_VIEW', 'SUPPLIER_MANAGE')")
    public ResponseEntity<List<SupplierResponse>> list() {
        return ResponseEntity.ok(catalogService.listSuppliers());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.can(authentication, 'SUPPLIER_MANAGE')")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createSupplier(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SUPPLIER_MANAGE')")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSupplierRequest req) {
        return ResponseEntity.ok(catalogService.updateSupplier(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SUPPLIER_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        catalogService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/import/template")
    @Operation(summary = "Download Excel template for bulk supplier import")
    @PreAuthorize("@authz.can(authentication, 'SUPPLIER_MANAGE')")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] bytes = supplierImportService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"supplier-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.can(authentication, 'SUPPLIER_MANAGE')")
    @Operation(summary = "Upload Excel to bulk-add suppliers")
    public ResponseEntity<SupplierImportResult> importSuppliers(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(supplierImportService.importFile(file));
    }
}
