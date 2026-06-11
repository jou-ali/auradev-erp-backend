package com.auradev.erp.creditnote.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only projection of a single line on a {@link com.auradev.erp.creditnote.entity.CreditNoteItem}.
 */
public record CreditNoteItemResponse(
        UUID id,
        UUID productId,
        String productNameSnapshot,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxableValue,
        BigDecimal gstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal lineTotal
) {}
