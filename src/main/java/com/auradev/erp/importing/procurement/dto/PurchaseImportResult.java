package com.auradev.erp.importing.procurement.dto;

import com.auradev.erp.importing.inventory.dto.ImportRowIssue;

import java.util.List;

public record PurchaseImportResult(
        int totalRows,
        int purchasesCreated,
        int suppliersCreated,
        int skippedInvalid,
        List<ImportRowIssue> issues
) {}
