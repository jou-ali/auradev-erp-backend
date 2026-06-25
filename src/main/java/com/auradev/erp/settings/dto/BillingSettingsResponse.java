package com.auradev.erp.settings.dto;

public record BillingSettingsResponse(
        int maxLineDiscountPercent,
        int maxBillDiscountPercent,
        int cashierMaxBillDiscountPercent,
        boolean allowHoldBill,
        boolean allowCreditSales,
        boolean showCashierOnReceipt,
        boolean showGstBreakupOnReceipt,
        boolean showCustomerOnReceipt,
        boolean roundTotalToRupee
) {}
