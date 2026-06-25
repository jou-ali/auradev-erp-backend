package com.auradev.erp.billing.dto;

import com.auradev.erp.billing.entity.BillPaymentStatus;
import com.auradev.erp.billing.entity.BillStatus;
import com.auradev.erp.billing.entity.DiscountMode;
import com.auradev.erp.billing.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BillResponse(
        UUID id,
        String billNo,
        UUID customerId,
        String customerName,
        String cashierName,
        BillStatus status,
        DiscountMode discountMode,
        String gstScheme,
        BigDecimal subtotal,
        BigDecimal billDiscount,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal grandTotal,
        BillPaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal tendered,
        BigDecimal changeDue,
        Instant createdAt,
        Instant updatedAt,
        List<GstSlabSummary> gstSlabs,
        List<BillLineResponse> lines
) {}
