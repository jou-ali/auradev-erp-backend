package com.auradev.erp.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for creating or updating a product category.
 *
 * @param name      display name; required, unique per tenant
 * @param sortOrder display position — lower values appear first; defaults to 0
 * @param active    whether the category is selectable in the catalog
 */
public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        @PositiveOrZero
        int sortOrder,

        Boolean active
) {}
