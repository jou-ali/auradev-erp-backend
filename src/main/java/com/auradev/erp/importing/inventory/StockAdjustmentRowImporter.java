package com.auradev.erp.importing.inventory;

import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.importing.inventory.dto.StockAdjustmentImportRow;
import com.auradev.erp.inventory.entity.MovementType;
import com.auradev.erp.inventory.entity.RefType;
import com.auradev.erp.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockAdjustmentRowImporter {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public enum Outcome { ADJUSTED, SKU_NOT_FOUND }

    public record ImportAttempt(Outcome outcome) {}

    public ImportAttempt importRow(StockAdjustmentImportRow row) {
        Product product = productRepository.findBySkuIgnoreCase(row.sku().trim())
                .orElse(null);
        if (product == null) {
            return new ImportAttempt(Outcome.SKU_NOT_FOUND);
        }

        MovementType movementType = StockAdjustmentMovementResolver.resolve(
                row.adjustment(), row.reason());

        String notes = row.notes() != null && !row.notes().isBlank()
                ? row.notes().trim()
                : "Bulk stock import row " + row.rowNumber();

        inventoryService.recordMovement(
                product.getId(),
                movementType,
                row.quantity(),
                RefType.manual,
                null,
                notes);

        return new ImportAttempt(Outcome.ADJUSTED);
    }
}
