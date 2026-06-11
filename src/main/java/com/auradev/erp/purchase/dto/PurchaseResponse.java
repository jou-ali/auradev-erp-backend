package com.auradev.erp.purchase.dto;

import com.auradev.erp.purchase.entity.PurchaseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API response payload representing a purchase / supplier bill.
 *
 * @param id           surrogate UUID
 * @param purchaseNo   human-readable purchase number, e.g. {@code ABC-2024-00001}
 * @param supplierId   UUID of the associated supplier
 * @param supplierName display name of the associated supplier
 * @param billDate     date on the supplier invoice
 * @param dueDate      payment due date; may be {@code null}
 * @param subtotal     sum of line amounts before tax
 * @param gstTotal     total GST across all lines
 * @param grandTotal   {@code subtotal + gstTotal}
 * @param status       current lifecycle status
 * @param notes        optional free-text notes
 * @param items        line-item details
 * @param createdAt    server timestamp of record creation
 * @param updatedAt    server timestamp of last update
 */
public record PurchaseResponse(
        UUID id,
        String purchaseNo,
        UUID supplierId,
        String supplierName,
        LocalDate billDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal gstTotal,
        BigDecimal grandTotal,
        PurchaseStatus status,
        String notes,
        List<PurchaseItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Line-item detail nested inside {@link PurchaseResponse}.
     *
     * @param id          surrogate UUID of the line item
     * @param productId   UUID of the catalogue product
     * @param productName display name of the catalogue product
     * @param quantity    number of units
     * @param rate        per-unit cost price excluding tax
     * @param gstRate     GST percentage
     * @param amount      taxable value ({@code quantity × rate})
     * @param gstAmount   GST amount for this line
     */
    public record PurchaseItemResponse(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal gstRate,
            BigDecimal amount,
            BigDecimal gstAmount
    ) {}
}
