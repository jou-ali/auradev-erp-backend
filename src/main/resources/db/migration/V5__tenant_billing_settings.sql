ALTER TABLE tenant_settings
    ADD COLUMN IF NOT EXISTS billing JSONB NOT NULL DEFAULT '{
        "maxLineDiscountPercent": 10,
        "maxBillDiscountPercent": 15,
        "cashierMaxBillDiscountPercent": 5,
        "allowHoldBill": true,
        "allowCreditSales": true,
        "showCashierOnReceipt": true,
        "showGstBreakupOnReceipt": true,
        "showCustomerOnReceipt": true,
        "roundTotalToRupee": false
    }'::jsonb;
