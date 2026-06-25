-- Extend default tax JSON with GST scheme fields (existing rows keep working via app defaults).
UPDATE tenant_settings
SET tax = tax || '{
    "scheme": "PRODUCT",
    "compositeRatePct": 5,
    "defaultCategoryRatePct": 5,
    "categoryRates": []
}'::jsonb
WHERE tax ->> 'scheme' IS NULL;
