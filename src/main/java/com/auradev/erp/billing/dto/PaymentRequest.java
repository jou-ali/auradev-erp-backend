package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Describes a single tender entry in a {@link CreateBillRequest}.
 *
 * <p>For split payments, the client sends multiple {@code PaymentRequest} entries;
 * the service will store each as a row with method {@code SPLIT_COMPONENT}.</p>
 *
 * @param method     the tender method
 * @param amount     the amount tendered via this method
 * @param tendered   cash tendered by the customer (CASH only; may be null)
 * @param reference  external reference (UPI ID, card approval code; may be null)
 */
public record PaymentRequest(

        @NotNull
        PaymentMethod method,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        BigDecimal tendered,

        String reference
) {}
