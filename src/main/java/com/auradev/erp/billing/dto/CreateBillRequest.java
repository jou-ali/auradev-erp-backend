package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.DiscountMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/bills}.
 *
 * @param customerId       the customer being billed; nullable for walk-in / anonymous sales
 * @param idempotencyKey   client-generated key for deduplication; if a bill with this key
 *                         already exists for the tenant the existing bill is returned as-is
 * @param items            one or more line items — must not be empty
 * @param payments         one or more payment entries that together cover the grand total
 * @param billDiscount     bill-level discount (0 if none; defaults to 0)
 * @param discountMode     whether {@code billDiscount} is AMOUNT or PERCENT
 */
public record CreateBillRequest(

        // Nullable — walk-in sales do not require a customer account
        UUID customerId,

        String idempotencyKey,

        @NotNull
        @NotEmpty
        @Valid
        List<BillItemRequest> items,

        BigDecimal billDiscount,

        DiscountMode discountMode,

        @NotNull
        @NotEmpty
        @Valid
        List<PaymentRequest> payments
) {}
