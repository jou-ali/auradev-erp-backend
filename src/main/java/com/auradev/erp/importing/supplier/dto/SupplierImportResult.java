package com.auradev.erp.importing.supplier.dto;

import com.auradev.erp.importing.inventory.dto.ImportRowIssue;

import java.util.List;

public record SupplierImportResult(
        int totalRows,
        int imported,
        int skippedDuplicates,
        int skippedInvalid,
        List<ImportRowIssue> issues
) {}
