package com.auradev.erp.common.pagination;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic wrapper for paginated API responses.
 *
 * <p>Usage:</p>
 * <pre>{@code
 *   Page<ProductDto> page = productService.findAll(pageable);
 *   return ResponseEntity.ok(PageResponse.of(page));
 * }</pre>
 *
 * @param <T> the type of content items
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Build a {@code PageResponse} from a Spring Data {@link Page}.
     *
     * @param page the Spring Data page returned by a repository or service
     * @param <T>  the content type
     * @return a populated {@code PageResponse}
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
