package com.auradev.erp.catalog.controller;

import com.auradev.erp.catalog.dto.CategoryResponse;
import com.auradev.erp.catalog.dto.CreateCategoryRequest;
import com.auradev.erp.catalog.dto.CreateProductRequest;
import com.auradev.erp.catalog.dto.ProductResponse;
import com.auradev.erp.catalog.service.CatalogService;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.inventory.dto.StockAdjustRequest;
import com.auradev.erp.inventory.entity.MovementReason;
import com.auradev.erp.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the catalog module — products and categories.
 *
 * <h2>Products — /api/v1/products</h2>
 * <ul>
 *   <li>GET    /                  — paginated product list with filters</li>
 *   <li>GET    /{id}              — single product by UUID</li>
 *   <li>GET    /barcode/{barcode} — POS scan lookup</li>
 *   <li>POST   /                  — create product</li>
 *   <li>PUT    /{id}              — full update</li>
 *   <li>DELETE /{id}              — soft-delete (active=false)</li>
 *   <li>POST   /{id}/stock-adjust — manual stock adjustment</li>
 *   <li>GET    /export/csv        — download product CSV</li>
 * </ul>
 *
 * <h2>Categories — /api/v1/categories</h2>
 * <ul>
 *   <li>GET    /           — full list for tenant (ordered by sort_order)</li>
 *   <li>POST   /           — create category</li>
 *   <li>PUT    /{id}       — update category</li>
 *   <li>DELETE /{id}       — soft-delete (active=false)</li>
 * </ul>
 */
@RequiredArgsConstructor
public class CatalogController {

    // =========================================================================
    // Products — /api/v1/products
    // =========================================================================

    @RestController
    @RequestMapping("/api/v1/products")
    @Tag(name = "Products", description = "Product catalog management — CRUD, barcode lookup, stock adjustment, CSV export")
    @RequiredArgsConstructor
    static class ProductController {

        private final CatalogService catalogService;

        @Operation(summary = "List products", description = "Paginated; filter by q (name/sku/barcode), category, unit, status (in|low|out|active|inactive)")
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<PageResponse<ProductResponse>> list(
                @RequestParam(required = false) String q,
                @RequestParam(required = false) String category,
                @RequestParam(required = false) String unit,
                @RequestParam(required = false) String status,
                @PageableDefault(size = 20, sort = "name") Pageable pageable) {

            UUID tenantId = TenantContext.require();
            return ResponseEntity.ok(catalogService.listProducts(tenantId, q, category, unit, status, pageable));
        }

        @Operation(summary = "Get product by ID")
        @GetMapping("/{id}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
            return ResponseEntity.ok(catalogService.getProduct(id));
        }

        @Operation(summary = "Lookup product by barcode (POS scan)")
        @GetMapping("/barcode/{barcode}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ProductResponse> getByBarcode(
                @Parameter(description = "Scanned barcode value") @PathVariable String barcode) {
            return ResponseEntity.ok(catalogService.getByBarcode(barcode));
        }

        @Operation(summary = "Create a new product")
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasAnyRole('MANAGER','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createProduct(req));
        }

        @Operation(summary = "Update a product (full replace)")
        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('MANAGER','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<ProductResponse> update(
                @PathVariable UUID id,
                @Valid @RequestBody CreateProductRequest req) {
            return ResponseEntity.ok(catalogService.updateProduct(id, req));
        }

        @Operation(summary = "Soft-delete a product")
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<Void> delete(@PathVariable UUID id) {
            catalogService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Manually adjust stock", description = "Use positive delta to add, negative to remove. Reason is required.")
        @PostMapping("/{id}/stock-adjust")
        @PreAuthorize("hasAnyRole('MANAGER','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<Void> adjustStock(
                @PathVariable UUID id,
                @Valid @RequestBody StockAdjustRequest req) {
            catalogService.adjustStock(id, req.delta(), req.reason(), req.notes());
            return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Export product catalog as CSV")
        @GetMapping("/export/csv")
        @PreAuthorize("hasAnyRole('MANAGER','ACCOUNTANT','TENANT_ADMIN','SUPER_ADMIN')")
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
    }

    // =========================================================================
    // Categories — /api/v1/categories
    // =========================================================================

    @RestController
    @RequestMapping("/api/v1/categories")
    @Tag(name = "Categories", description = "Product category management")
    @RequiredArgsConstructor
    static class CategoryController {

        private final CatalogService catalogService;

        @Operation(summary = "List all categories for the tenant")
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<List<CategoryResponse>> list() {
            return ResponseEntity.ok(catalogService.listCategories());
        }

        @Operation(summary = "Create a new category")
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasAnyRole('MANAGER','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(req));
        }

        @Operation(summary = "Update a category")
        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('MANAGER','INVENTORY_STAFF','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<CategoryResponse> update(
                @PathVariable UUID id,
                @Valid @RequestBody CreateCategoryRequest req) {
            return ResponseEntity.ok(catalogService.updateCategory(id, req));
        }

        @Operation(summary = "Soft-delete a category (sets active=false)")
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('MANAGER','TENANT_ADMIN','SUPER_ADMIN')")
        public ResponseEntity<Void> delete(@PathVariable UUID id) {
            catalogService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        }
    }
}
