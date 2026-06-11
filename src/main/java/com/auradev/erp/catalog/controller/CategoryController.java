package com.auradev.erp.catalog.controller;

import com.auradev.erp.catalog.dto.CategoryResponse;
import com.auradev.erp.catalog.dto.CreateCategoryRequest;
import com.auradev.erp.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Product category management")
@RequiredArgsConstructor
public class CategoryController {

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
