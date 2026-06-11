package com.auradev.erp.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String name,
        String slug,
        String parentId,
        Boolean isActive
) {}
