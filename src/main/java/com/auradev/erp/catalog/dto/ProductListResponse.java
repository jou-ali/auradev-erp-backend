package com.auradev.erp.catalog.dto;

import com.auradev.erp.common.pagination.PageResponse;

/**
 * Typed alias for a paginated list of {@link ProductResponse} items.
 *
 * <p>Using a dedicated type alias (rather than raw {@code PageResponse<ProductResponse>})
 * allows Jackson to serialize/deserialize the generic type arguments correctly
 * without requiring a {@code TypeReference} at every call site, and gives
 * OpenAPI tooling a named schema to reference in generated documentation.</p>
 *
 * <p>Construct via {@link PageResponse#of(org.springframework.data.domain.Page)}:</p>
 * <pre>{@code
 *   Page<ProductResponse> page = ...;
 *   ProductListResponse resp = new ProductListResponse(
 *       page.getContent(), page.getNumber(), page.getSize(),
 *       page.getTotalElements(), page.getTotalPages());
 * }</pre>
 */
public record ProductListResponse(
        java.util.List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Convenience factory that wraps a Spring Data {@link org.springframework.data.domain.Page}.
     *
     * @param page the Spring Data page of product responses
     * @return a populated {@code ProductListResponse}
     */
    public static ProductListResponse of(org.springframework.data.domain.Page<ProductResponse> page) {
        return new ProductListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
