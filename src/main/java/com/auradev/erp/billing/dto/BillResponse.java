package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.BillStatus;
import com.auradev.erp.billing.entity.DiscountMode;
import com.auradev.erp.billing.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full API representation of a completed sales bill, including line-item and
 * per-tax-head breakdowns.
 *
 * @param id                  bill UUID
 * @param billNo              formatted bill number (e.g. {@code ERP-2526-00042})
 * @param tenantId            owning tenant
 * @param customerId          the customer billed
 * @param cashierId           the cashier who raised the bill
 * @param placeOfSupplyState  2-char state code used for GST (intra/inter)
 * @param subtotal            sum of all line taxable values
 * @param billDiscount        bill-level discount applied
 * @param discountMode        how the bill discount is expressed
 * @param cgstTotal           total CGST across all lines
 * @param sgstTotal           total SGST across all lines
 * @param igstTotal           total IGST across all lines
 * @param roundOff            whole-rupee adjustment
 * @param grandTotal          amount the customer pays
 * @param paymentStatus       settlement status
 * @param status              lifecycle status
 * @param idempotencyKey      client-supplied deduplication key echoed back in the response
 * @param receiptUrl          URL of the generated PDF receipt (may be null)
 * @param items               line-item breakdown with per-line tax details
 * @param payments            payment tender rows
 * @param createdAt           timestamp when the bill was created
 */
public record BillResponse(
        UUID id,
        String billNo,
        UUID tenantId,
        UUID customerId,
        String customerName,
        UUID cashierId,
        String cashierName,
        String placeOfSupplyState,
        BigDecimal subtotal,
        BigDecimal billDiscount,
        DiscountMode discountMode,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal roundOff,
        BigDecimal grandTotal,
        PaymentStatus paymentStatus,
        BillStatus status,
        String idempotencyKey,
        String receiptUrl,
        List<BillItemResponse> items,
        List<PaymentResponse> payments,
        Instant createdAt
) {}
