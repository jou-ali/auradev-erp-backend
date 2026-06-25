package com.auradev.erp.catalog.controller;

import com.auradev.erp.catalog.dto.CreateProductRequest;
import com.auradev.erp.catalog.dto.ProductResponse;
import com.auradev.erp.catalog.dto.UpdateProductRequest;
import com.auradev.erp.catalog.service.CatalogService;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.inventory.dto.ProductStockAdjustBody;
import com.auradev.erp.inventory.dto.StockMovementResponse;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Shared catalogue + tenant inventory (schema v2.0)")
@RequiredArgsConstructor
public class ProductController {

    private final CatalogService catalogService;
    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'INVENTORY_VIEW')")
    public ResponseEntity<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(catalogService.listProducts(
                TenantContext.require(), q, category, unit, status, pageable));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("@authz.can(authentication, 'INVENTORY_EXPORT')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.require();
        byte[] csv = catalogService.exportCsv(tenantId, q, category, unit, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("@authz.canAny(authentication, 'INVENTORY_VIEW', 'BILL_CREATE')")
    public ResponseEntity<ProductResponse> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(catalogService.getByBarcode(barcode));
    }

    @GetMapping("/pos/quick-picks")
    @Operation(summary = "POS quick picks", description = "Top sellers (7 days) with catalogue fallback for scanner-first checkout")
    @PreAuthorize("@authz.can(authentication, 'BILL_CREATE')")
    public ResponseEntity<List<ProductResponse>> posQuickPicks(
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(catalogService.listPosQuickPicks(TenantContext.require(), limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'INVENTORY_VIEW')")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getProduct(id));
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("@authz.can(authentication, 'INVENTORY_VIEW')")
    public ResponseEntity<PageResponse<StockMovementResponse>> movements(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getMovements(id, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.canAny(authentication, 'INVENTORY_EDIT', 'PRODUCT_MANAGE')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createProduct(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canAny(authentication, 'INVENTORY_EDIT', 'PRODUCT_MANAGE')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) {
        return ResponseEntity.ok(catalogService.updateProduct(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'PRODUCT_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        catalogService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock-adjust")
    @PreAuthorize("@authz.can(authentication, 'INVENTORY_EDIT')")
    public ResponseEntity<Void> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStockAdjustBody req) {
        catalogService.adjustStock(id, req.movementType(), req.quantity(), req.notes());
        return ResponseEntity.noContent().build();
    }
}
