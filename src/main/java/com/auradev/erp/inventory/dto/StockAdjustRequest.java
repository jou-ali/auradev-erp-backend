package com.auradev.erp.inventory.dto;

import com.auradev.erp.inventory.entity.MovementReason;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for a manual stock adjustment via the inventory API.
 *
 * <p>Use a positive {@code delta} to add stock (e.g. after a count
 * reconciliation reveals more items than recorded) and a negative
 * {@code delta} to remove stock (e.g. damage write-off).</p>
 *
 * @param productId UUID of the product to adjust; required
 * @param delta     signed quantity change; required (zero is technically valid
 *                  but will result in a no-op movement record)
 * @param reason    business reason for the adjustment; required
 * @param notes     optional free-text explanation visible in the audit ledger
 */
public record StockAdjustRequest(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Delta is required")
        BigDecimal delta,

        @NotNull(message = "Reason is required")
        MovementReason reason,

        String notes
) {
}
