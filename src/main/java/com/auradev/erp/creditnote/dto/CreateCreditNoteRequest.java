package com.auradev.erp.creditnote.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/credit-notes}.
 *
 * @param originalBillId the UUID of the bill being credited; required
 * @param reason         free-text explanation for the return; required
 * @param items          one or more lines being returned; must not be empty
 */
public record CreateCreditNoteRequest(
        @NotNull(message = "Original bill ID is required")
        UUID originalBillId,

        @NotBlank(message = "Reason is required")
        String reason,

        @NotNull @NotEmpty(message = "At least one item is required")
        @Valid
        List<CreditNoteItemRequest> items
) {}
