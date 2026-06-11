package com.auradev.erp.creditnote.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line item being returned on a credit note.
 *
 * @param productId UUID of the product being returned; must exist on the original bill
 * @param quantity  returned quantity; must be positive
 * @param unitPrice selling price at which the item was originally billed
 */
public record CreditNoteItemRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @Positive(message = "Unit price must be positive")
        BigDecimal unitPrice
) {}
