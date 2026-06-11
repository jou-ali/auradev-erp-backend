package com.auradev.erp.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.auradev.erp.billing.entity.BillItem}.
 *
 * @param id                  line item UUID
 * @param productId           the product sold
 * @param productNameSnapshot product name captured at bill time
 * @param skuSnapshot         SKU captured at bill time
 * @param hsnSnapshot         HSN/SAC code captured at bill time
 * @param quantity            quantity sold
 * @param unitPrice           price per unit used for this bill
 * @param lineDiscount        line-level discount applied
 * @param taxableValue        net taxable amount for this line
 * @param gstRate             GST rate percentage
 * @param cgstAmount          CGST amount (intra-state)
 * @param sgstAmount          SGST amount (intra-state)
 * @param igstAmount          IGST amount (inter-state)
 * @param lineTotal           taxableValue + all taxes
 */
public record BillItemResponse(
        UUID id,
        UUID productId,
        String productNameSnapshot,
        String skuSnapshot,
        String hsnSnapshot,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal taxableValue,
        BigDecimal gstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal lineTotal
) {}
