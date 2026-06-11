package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.auradev.erp.billing.entity.Payment}.
 *
 * @param id        payment row UUID
 * @param method    tender method
 * @param amount    amount settled via this payment row
 * @param tendered  cash tendered (CASH payments only; null otherwise)
 * @param changeDue change to return to the customer
 * @param reference external reference (UPI ID, card approval code, etc.)
 */
public record PaymentResponse(
        UUID id,
        PaymentMethod method,
        BigDecimal amount,
        BigDecimal tendered,
        BigDecimal changeDue,
        String reference
) {}
