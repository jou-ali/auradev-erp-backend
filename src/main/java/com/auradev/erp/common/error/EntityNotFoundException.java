package com.auradev.erp.common.error;

/**
 * Thrown when a requested entity cannot be found in the data store.
 *
 * <p>Mapped to HTTP 404 by {@link GlobalExceptionHandler}.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 *   throw new EntityNotFoundException("Product", productId);
 * }</pre>
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * @param entityType the simple class/domain name of the entity
     *                   (e.g. {@code "Product"}, {@code "Customer"})
     * @param id         the identifier that was not found; {@code toString()} is
     *                   called to embed it in the message
     */
    public EntityNotFoundException(String entityType, Object id) {
        super(entityType + " not found: " + id);
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
