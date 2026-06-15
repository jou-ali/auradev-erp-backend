package com.auradev.erp.importing.inventory;

import com.auradev.erp.importing.inventory.dto.StockAdjustmentImportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockAdjustmentRowImportExecutor {

    private final StockAdjustmentRowImporter rowImporter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockAdjustmentRowImporter.ImportAttempt importOne(StockAdjustmentImportRow row) {
        return rowImporter.importRow(row);
    }
}
