package com.auradev.erp.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateBillingSettingsRequest(
        @Min(0) @Max(100) Integer maxLineDiscountPercent,
        @Min(0) @Max(100) Integer maxBillDiscountPercent,
        @Min(0) @Max(100) Integer cashierMaxBillDiscountPercent,
        Boolean allowHoldBill,
        Boolean allowCreditSales,
        Boolean showCashierOnReceipt,
        Boolean showGstBreakupOnReceipt,
        Boolean showCustomerOnReceipt,
        Boolean roundTotalToRupee
) {}
