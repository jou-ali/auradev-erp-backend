package com.auradev.erp.importing.procurement;

import com.auradev.erp.importing.procurement.dto.PurchaseImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases/import")
@Tag(name = "Purchase Import", description = "Bulk purchase bills from Excel")
@RequiredArgsConstructor
public class PurchaseImportController {

    private final PurchaseImportService importService;

    @GetMapping("/template")
    @Operation(summary = "Download Excel template for bulk purchase import")
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    public ResponseEntity<byte[]> template() {
        byte[] bytes = importService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"purchase-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.can(authentication, 'PURCHASE_MANAGE')")
    @Operation(summary = "Upload Excel to create draft purchase bills for one supplier")
    public ResponseEntity<PurchaseImportResult> upload(
            @RequestParam("supplierId") UUID supplierId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importFile(file, supplierId));
    }
}
