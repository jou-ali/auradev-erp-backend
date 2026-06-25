package com.auradev.erp.settings.model;

/**
 * POS / billing behaviour stored in {@code tenant_settings.billing} JSONB.
 */
public record BillingConfig(
        int maxLineDiscountPercent,
        int maxBillDiscountPercent,
        int cashierMaxBillDiscountPercent,
        boolean allowHoldBill,
        boolean allowCreditSales,
        boolean showCashierOnReceipt,
        boolean showGstBreakupOnReceipt,
        boolean showCustomerOnReceipt,
        boolean roundTotalToRupee
) {
    public static BillingConfig defaults() {
        return new BillingConfig(10, 15, 5, true, true, true, true, true, false);
    }

    public BillingConfig normalized() {
        return new BillingConfig(
                clamp(maxLineDiscountPercent, 0, 100),
                clamp(maxBillDiscountPercent, 0, 100),
                clamp(cashierMaxBillDiscountPercent, 0, 100),
                allowHoldBill,
                allowCreditSales,
                showCashierOnReceipt,
                showGstBreakupOnReceipt,
                showCustomerOnReceipt,
                roundTotalToRupee);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
