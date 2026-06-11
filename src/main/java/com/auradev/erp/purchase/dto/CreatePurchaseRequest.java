package com.auradev.erp.purchase.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a new purchase / supplier bill.
 *
 * @param supplierId UUID of the supplier
 * @param billDate   date shown on the supplier's invoice
 * @param dueDate    payment due date; may be {@code null}
 * @param items      one or more line items (must not be empty)
 * @param notes      optional free-text notes
 */
public record CreatePurchaseRequest(

        @NotNull(message = "supplierId is required")
        UUID supplierId,

        @NotNull(message = "billDate is required")
        LocalDate billDate,

        LocalDate dueDate,

        @NotEmpty(message = "At least one item is required")
        List<PurchaseItemRequest> items,

        String notes
) {

    /**
     * A single line-item within a {@link CreatePurchaseRequest}.
     *
     * @param productId UUID of the catalogue product
     * @param quantity  number of units (supports fractional quantities)
     * @param rate      per-unit cost price excluding GST
     * @param gstRate   GST percentage, e.g. {@code 18.00} for 18 %
     */
    public record PurchaseItemRequest(
            @NotNull(message = "productId is required")
            UUID productId,

            @NotNull(message = "quantity is required")
            BigDecimal quantity,

            @NotNull(message = "rate is required")
            BigDecimal rate,

            @NotNull(message = "gstRate is required")
            BigDecimal gstRate
    ) {}
}
