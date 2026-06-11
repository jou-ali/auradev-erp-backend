-- =============================================================
-- AuraDev ERP — Schema v2.0 (PDF) catalog + inventory migration
-- Shared catalog: categories, suppliers, products (no tenant_id)
-- Tenant-scoped: inventory, stock_movements
-- =============================================================

-- New enum types (PDF v2.0)
DO $$ BEGIN CREATE TYPE unit_type AS ENUM ('unit', 'weight_kg', 'weight_g', 'volume_l', 'volume_ml');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE movement_type AS ENUM ('sale', 'purchase', 'adjustment_in', 'adjustment_out', 'return', 'waste');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE ref_type AS ENUM ('bill', 'purchase_order', 'manual');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- -------------------------------------------------------------
-- inventory table (tenant-scoped stock)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    product_id          UUID NOT NULL REFERENCES products(id),
    quantity_on_hand    NUMERIC(12,3) NOT NULL DEFAULT 0,
    low_stock_threshold NUMERIC(12,3),
    reorder_quantity    NUMERIC(12,3),
    last_updated        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_inventory_tenant_product ON inventory(tenant_id, product_id);

-- Seed inventory from legacy product stock columns (if present)
INSERT INTO inventory (tenant_id, product_id, quantity_on_hand, low_stock_threshold, reorder_quantity, last_updated)
SELECT p.tenant_id, p.id, p.current_stock, p.reorder_level, p.reorder_level, COALESCE(p.updated_at, NOW())
FROM products p
WHERE p.tenant_id IS NOT NULL
ON CONFLICT (tenant_id, product_id) DO NOTHING;

-- -------------------------------------------------------------
-- products — add v2 columns, migrate data
-- -------------------------------------------------------------
ALTER TABLE products ADD COLUMN IF NOT EXISTS unit_type unit_type;
ALTER TABLE products ADD COLUMN IF NOT EXISTS unit_label VARCHAR(20);
ALTER TABLE products ADD COLUMN IF NOT EXISTS price_mrp NUMERIC(10,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS price_selling NUMERIC(10,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS tax_rate_pct NUMERIC(5,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_active BOOLEAN;
ALTER TABLE products ADD COLUMN IF NOT EXISTS supplier_id UUID;

-- Migrate from legacy column names
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'mrp') THEN
        UPDATE products SET price_mrp = mrp WHERE price_mrp IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'selling_price') THEN
        UPDATE products SET price_selling = selling_price WHERE price_selling IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'gst_rate') THEN
        UPDATE products SET tax_rate_pct = gst_rate WHERE tax_rate_pct IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'active') THEN
        UPDATE products SET is_active = active WHERE is_active IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'unit') THEN
        UPDATE products SET unit_type = CASE
            WHEN unit::text = 'kg' THEN 'weight_kg'::unit_type
            ELSE 'unit'::unit_type
        END WHERE unit_type IS NULL;
        UPDATE products SET unit_label = CASE
            WHEN unit::text = 'kg' THEN 'kg'
            ELSE 'pcs'
        END WHERE unit_label IS NULL;
    END IF;
END $$;

UPDATE products SET unit_type = 'unit'::unit_type WHERE unit_type IS NULL;
UPDATE products SET unit_label = 'pcs' WHERE unit_label IS NULL;
UPDATE products SET is_active = TRUE WHERE is_active IS NULL;

ALTER TABLE products ALTER COLUMN unit_type SET NOT NULL;
ALTER TABLE products ALTER COLUMN unit_label SET NOT NULL;
ALTER TABLE products ALTER COLUMN price_mrp SET NOT NULL;
ALTER TABLE products ALTER COLUMN price_selling SET NOT NULL;
ALTER TABLE products ALTER COLUMN tax_rate_pct SET NOT NULL;
ALTER TABLE products ALTER COLUMN is_active SET NOT NULL;

-- Drop legacy stock columns (now in inventory)
ALTER TABLE products DROP COLUMN IF EXISTS current_stock;
ALTER TABLE products DROP COLUMN IF EXISTS reorder_level;
ALTER TABLE products DROP COLUMN IF EXISTS unit;
ALTER TABLE products DROP COLUMN IF EXISTS mrp;
ALTER TABLE products DROP COLUMN IF EXISTS selling_price;
ALTER TABLE products DROP COLUMN IF EXISTS gst_rate;
ALTER TABLE products DROP COLUMN IF EXISTS active;
ALTER TABLE products DROP COLUMN IF EXISTS hsn_code;
ALTER TABLE products DROP COLUMN IF EXISTS version;
ALTER TABLE products DROP COLUMN IF EXISTS updated_at;
ALTER TABLE products DROP COLUMN IF EXISTS created_by;
ALTER TABLE products DROP COLUMN IF EXISTS updated_by;

-- Global SKU/barcode uniqueness (PDF v2)
DROP INDEX IF EXISTS idx_products_barcode_unique;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_tenant_id_sku_key;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_tenant_id_barcode_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL;

-- Remove tenant_id from shared catalog products
ALTER TABLE products DROP COLUMN IF EXISTS tenant_id;

-- -------------------------------------------------------------
-- categories — v2 shared catalog
-- -------------------------------------------------------------
ALTER TABLE categories ADD COLUMN IF NOT EXISTS slug VARCHAR(120);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES categories(id);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_active BOOLEAN;

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'categories' AND column_name = 'active') THEN
        UPDATE categories SET is_active = active WHERE is_active IS NULL;
    END IF;
END $$;

UPDATE categories SET is_active = TRUE WHERE is_active IS NULL;
UPDATE categories SET slug = lower(regexp_replace(trim(name), '[^a-zA-Z0-9]+', '-', 'g'))
WHERE slug IS NULL OR slug = '';

-- Ensure unique slugs
UPDATE categories c SET slug = slug || '-' || left(c.id::text, 8)
WHERE slug IN (SELECT slug FROM categories GROUP BY slug HAVING count(*) > 1);

ALTER TABLE categories ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE categories ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);

ALTER TABLE categories DROP COLUMN IF EXISTS sort_order;
ALTER TABLE categories DROP COLUMN IF EXISTS active;
ALTER TABLE categories DROP COLUMN IF EXISTS version;
ALTER TABLE categories DROP COLUMN IF EXISTS updated_at;
ALTER TABLE categories DROP COLUMN IF EXISTS created_by;
ALTER TABLE categories DROP COLUMN IF EXISTS updated_by;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_tenant_id_name_key;
ALTER TABLE categories DROP COLUMN IF EXISTS tenant_id;

-- -------------------------------------------------------------
-- suppliers — reshape to shared catalog (keep existing rows, drop tenant scope)
-- -------------------------------------------------------------
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS contact_person VARCHAR(120);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'suppliers' AND column_name = 'is_active') THEN
        ALTER TABLE suppliers ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END $$;

UPDATE suppliers SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE suppliers DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE suppliers DROP COLUMN IF EXISTS category_id;
ALTER TABLE suppliers DROP COLUMN IF EXISTS state_code;
ALTER TABLE suppliers DROP COLUMN IF EXISTS payment_terms_days;
ALTER TABLE suppliers DROP COLUMN IF EXISTS version;
ALTER TABLE suppliers DROP COLUMN IF EXISTS updated_at;
ALTER TABLE suppliers DROP COLUMN IF EXISTS created_by;
ALTER TABLE suppliers DROP COLUMN IF EXISTS updated_by;

ALTER TABLE products
    ADD CONSTRAINT products_supplier_id_fkey
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

-- -------------------------------------------------------------
-- stock_movements — v2 columns
-- -------------------------------------------------------------
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS movement_type movement_type;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS quantity NUMERIC(12,3);
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS quantity_after NUMERIC(12,3);

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'stock_movements' AND column_name = 'delta') THEN
        UPDATE stock_movements SET quantity = ABS(delta) WHERE quantity IS NULL;
        UPDATE stock_movements SET movement_type = CASE
            WHEN reason::text = 'SALE' AND delta < 0 THEN 'sale'::movement_type
            WHEN reason::text = 'SALE' AND delta > 0 THEN 'return'::movement_type
            WHEN reason::text = 'RETURN' THEN 'return'::movement_type
            WHEN reason::text = 'GRN' THEN 'purchase'::movement_type
            WHEN reason::text = 'DAMAGE' THEN 'waste'::movement_type
            WHEN reason::text = 'COUNT_CORRECTION' AND delta >= 0 THEN 'adjustment_in'::movement_type
            WHEN reason::text = 'COUNT_CORRECTION' AND delta < 0 THEN 'adjustment_out'::movement_type
            WHEN reason::text = 'MANUAL_ADJUST' AND delta >= 0 THEN 'adjustment_in'::movement_type
            ELSE 'adjustment_out'::movement_type
        END WHERE movement_type IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'stock_movements' AND column_name = 'balance_after') THEN
        UPDATE stock_movements SET quantity_after = balance_after WHERE quantity_after IS NULL;
    END IF;
END $$;

UPDATE stock_movements SET movement_type = 'adjustment_in'::movement_type WHERE movement_type IS NULL;
UPDATE stock_movements SET quantity = 0 WHERE quantity IS NULL;
UPDATE stock_movements SET quantity_after = 0 WHERE quantity_after IS NULL;

ALTER TABLE stock_movements ALTER COLUMN movement_type SET NOT NULL;
ALTER TABLE stock_movements ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE stock_movements ALTER COLUMN quantity_after SET NOT NULL;

-- Map reference_type to ref_type enum values
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS reference_type_v2 ref_type;
UPDATE stock_movements SET reference_type_v2 = CASE reference_type::text
    WHEN 'BILL' THEN 'bill'::ref_type
    WHEN 'PURCHASE' THEN 'purchase_order'::ref_type
    ELSE 'manual'::ref_type
END WHERE reference_type IS NOT NULL;
UPDATE stock_movements SET reference_type_v2 = 'manual'::ref_type WHERE reference_type_v2 IS NULL AND reference_type IS NULL;

ALTER TABLE stock_movements DROP COLUMN IF EXISTS reference_type;
ALTER TABLE stock_movements RENAME COLUMN reference_type_v2 TO reference_type;

ALTER TABLE stock_movements DROP COLUMN IF EXISTS delta;
ALTER TABLE stock_movements DROP COLUMN IF EXISTS reason;
ALTER TABLE stock_movements DROP COLUMN IF EXISTS balance_after;

-- created_by required in v2
UPDATE stock_movements SET created_by = (SELECT id FROM users LIMIT 1) WHERE created_by IS NULL;
ALTER TABLE stock_movements ALTER COLUMN created_by SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_movements_tenant_product_created
    ON stock_movements(tenant_id, product_id, created_at DESC);
