package com.auradev.erp.importing.inventory;

import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.CategoryRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.importing.inventory.dto.InventoryImportRow;
import com.auradev.erp.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryRowImporter {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryProvisioner categoryProvisioner;
    private final InventoryService inventoryService;

    public enum Outcome { IMPORTED, DUPLICATE_SKU, DUPLICATE_BARCODE }

    public record ImportAttempt(Outcome outcome, boolean categoryCreated) {}

    public ImportAttempt importRow(InventoryImportRow row, UUID tenantId, ImportBatchContext batch) {
        String sku = row.sku().trim();

        if (batch.seenSku(sku) || productRepository.existsBySku(sku)) {
            return new ImportAttempt(Outcome.DUPLICATE_SKU, false);
        }

        String barcode = blankToNull(row.barcode());
        if (barcode != null
                && (batch.seenBarcode(barcode) || productRepository.existsByBarcode(barcode))) {
            return new ImportAttempt(Outcome.DUPLICATE_BARCODE, false);
        }

        boolean categoryCreated = false;
        Category category = batch.cachedCategory(row.categoryName());
        if (category == null) {
            boolean existed = categoryRepository.findByNameIgnoreCase(row.categoryName().trim()).isPresent();
            category = categoryProvisioner.resolveOrCreate(row.categoryName());
            categoryCreated = !existed;
            batch.cacheCategory(row.categoryName(), category);
        }

        Product product = new Product();
        product.setName(row.name().trim());
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setCategory(category);
        product.setUnitType(row.unitType());
        product.setUnitLabel(row.unitLabel());
        product.setPriceMrp(row.mrp());
        product.setPriceSelling(row.sellingPrice());
        product.setCostPrice(row.costPrice());
        product.setTaxRatePct(Optional.ofNullable(row.taxRatePct()).orElse(BigDecimal.ZERO));
        product.setActive(true);

        Product saved = productRepository.save(product);

        BigDecimal stock = Optional.ofNullable(row.initialStock()).orElse(BigDecimal.ZERO);
        BigDecimal reorder = row.reorderLevel();
        inventoryService.ensureInventoryRow(tenantId, saved.getId(), stock, reorder, reorder);

        batch.markSku(sku);
        if (barcode != null) {
            batch.markBarcode(barcode);
        }

        return new ImportAttempt(Outcome.IMPORTED, categoryCreated);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
