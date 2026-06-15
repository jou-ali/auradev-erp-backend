package com.auradev.erp.importing.inventory.dto;

import java.util.List;

public record InventoryImportResult(
        int totalRows,
        int imported,
        int stockAdjusted,
        int skippedDuplicates,
        int skippedInvalid,
        int categoriesCreated,
        List<ImportRowIssue> issues
) {}
