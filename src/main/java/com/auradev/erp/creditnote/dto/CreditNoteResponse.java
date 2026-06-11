package com.auradev.erp.creditnote.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.auradev.erp.creditnote.entity.CreditNote}.
 */
public record CreditNoteResponse(
        UUID id,
        String creditNoteNo,
        UUID originalBillId,
        String originalBillNo,
        UUID customerId,
        String customerName,
        String reason,
        BigDecimal subtotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal grandTotal,
        List<CreditNoteItemResponse> items,
        Instant createdAt,
        UUID createdBy
) {}
