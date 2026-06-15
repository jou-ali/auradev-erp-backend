package com.auradev.erp.importing.inventory;

import com.auradev.erp.importing.inventory.dto.InventoryImportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Runs each import row in its own transaction so one failure does not abort the batch. */
@Component
@RequiredArgsConstructor
public class InventoryRowImportExecutor {

    private final InventoryRowImporter rowImporter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InventoryRowImporter.ImportAttempt importOne(
            InventoryImportRow row,
            UUID tenantId,
            ImportBatchContext batch) {
        return rowImporter.importRow(row, tenantId, batch);
    }
}
