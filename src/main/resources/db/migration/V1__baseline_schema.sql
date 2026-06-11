-- =============================================================
-- AuraDev Commerce ERP — Baseline Schema
-- Flyway V1 — run once; never alter this file after deploy.
-- All business tables carry tenant_id for shared-schema multi-tenancy.
-- =============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================
-- TENANTS & STORES
-- =============================================================
CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    phone           TEXT,
    gstin           TEXT,
    state_code      CHAR(2) NOT NULL DEFAULT '29',  -- 29 = Karnataka
    address         TEXT,
    bill_no_prefix  TEXT NOT NULL DEFAULT 'ERP',
    bill_footer     TEXT,
    logo_url        TEXT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================
-- USERS
-- =============================================================
CREATE TYPE user_role AS ENUM (
    'SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'CASHIER', 'INVENTORY_STAFF', 'ACCOUNTANT'
);
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID REFERENCES tenants(id),   -- NULL for SUPER_ADMIN
    name            TEXT NOT NULL,
    email           TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    role            user_role NOT NULL,
    status          user_status NOT NULL DEFAULT 'ACTIVE',
    invite_token    TEXT,
    invite_expires  TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    UNIQUE (tenant_id, email)
);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);

-- Refresh tokens (hashed, rotating)
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- =============================================================
-- CATEGORIES
-- =============================================================
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    name        TEXT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  UUID REFERENCES users(id),
    updated_by  UUID REFERENCES users(id),
    UNIQUE (tenant_id, name)
);
CREATE INDEX idx_categories_tenant ON categories(tenant_id);

-- =============================================================
-- PRODUCTS
-- =============================================================
CREATE TYPE product_unit AS ENUM ('pcs', 'kg');

CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    category_id     UUID REFERENCES categories(id),
    name            TEXT NOT NULL,
    sku             TEXT NOT NULL,
    barcode         TEXT,
    unit            product_unit NOT NULL DEFAULT 'pcs',
    mrp             NUMERIC(12,2) NOT NULL,
    selling_price   NUMERIC(12,2) NOT NULL,
    cost_price      NUMERIC(12,2),
    gst_rate        NUMERIC(4,2) NOT NULL DEFAULT 0,   -- 0 / 5 / 12 / 18
    hsn_code        TEXT,
    current_stock   NUMERIC(12,3) NOT NULL DEFAULT 0,
    reorder_level   NUMERIC(12,3) NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    UNIQUE (tenant_id, sku),
    UNIQUE (tenant_id, barcode) -- partial unique enforced via index below
);
-- Partial unique index: barcode unique per tenant only when not null
DROP INDEX IF EXISTS idx_products_barcode_unique;
CREATE UNIQUE INDEX idx_products_barcode_unique ON products(tenant_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(tenant_id, active);

-- =============================================================
-- STOCK MOVEMENTS (immutable ledger — no UPDATE/DELETE)
-- =============================================================
CREATE TYPE movement_reason AS ENUM (
    'SALE', 'RETURN', 'GRN', 'DAMAGE', 'COUNT_CORRECTION', 'MANUAL_ADJUST'
);
CREATE TYPE movement_ref_type AS ENUM (
    'BILL', 'PURCHASE', 'ADJUSTMENT', 'CREDIT_NOTE'
);

CREATE TABLE stock_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    product_id      UUID NOT NULL REFERENCES products(id),
    delta           NUMERIC(12,3) NOT NULL,         -- + in / - out
    reason          movement_reason NOT NULL,
    reference_type  movement_ref_type,
    reference_id    UUID,
    balance_after   NUMERIC(12,3) NOT NULL,
    notes           TEXT,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_movements_product ON stock_movements(product_id, created_at DESC);
CREATE INDEX idx_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX idx_movements_reference ON stock_movements(reference_id) WHERE reference_id IS NOT NULL;

-- =============================================================
-- CUSTOMERS & SUPPLIERS (party module)
-- =============================================================
CREATE TYPE customer_type AS ENUM ('walkin', 'b2c', 'b2b');

CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    name            TEXT NOT NULL,
    phone           TEXT,
    email           TEXT,
    type            customer_type NOT NULL DEFAULT 'walkin',
    gstin           TEXT,
    state_code      CHAR(2),
    address         TEXT,
    loyalty_points  INT NOT NULL DEFAULT 0,
    credit_balance  NUMERIC(12,2) NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id)
);
CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_customers_phone ON customers(tenant_id, phone) WHERE phone IS NOT NULL;

CREATE TABLE suppliers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    name                TEXT NOT NULL,
    category_id         UUID REFERENCES categories(id),
    gstin               TEXT,
    state_code          CHAR(2),
    phone               TEXT,
    email               TEXT,
    address             TEXT,
    payment_terms_days  INT NOT NULL DEFAULT 30,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID REFERENCES users(id),
    updated_by          UUID REFERENCES users(id)
);
CREATE INDEX idx_suppliers_tenant ON suppliers(tenant_id);

-- =============================================================
-- BILL SEQUENCES (gapless numbering per tenant per FY)
-- =============================================================
CREATE TABLE bill_sequences (
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    fy          CHAR(4) NOT NULL,   -- e.g. '2025'
    last_no     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, fy)
);

CREATE TABLE purchase_sequences (
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    fy          CHAR(4) NOT NULL,
    last_no     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, fy)
);

CREATE TABLE credit_note_sequences (
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    fy          CHAR(4) NOT NULL,
    last_no     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, fy)
);

-- =============================================================
-- BILLING (SALES)
-- =============================================================
CREATE TYPE bill_payment_status AS ENUM ('PAID', 'PARTIAL', 'CREDIT', 'VOID');
CREATE TYPE bill_status AS ENUM ('COMPLETED', 'HELD', 'VOID');
CREATE TYPE discount_mode AS ENUM ('AMOUNT', 'PERCENT');

CREATE TABLE bills (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    bill_no                 TEXT NOT NULL,
    customer_id             UUID NOT NULL REFERENCES customers(id),
    cashier_id              UUID NOT NULL REFERENCES users(id),
    place_of_supply_state   CHAR(2),
    subtotal                NUMERIC(12,2) NOT NULL DEFAULT 0,
    bill_discount           NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_mode           discount_mode NOT NULL DEFAULT 'AMOUNT',
    cgst_total              NUMERIC(12,2) NOT NULL DEFAULT 0,
    sgst_total              NUMERIC(12,2) NOT NULL DEFAULT 0,
    igst_total              NUMERIC(12,2) NOT NULL DEFAULT 0,
    round_off               NUMERIC(12,2) NOT NULL DEFAULT 0,
    grand_total             NUMERIC(12,2) NOT NULL DEFAULT 0,
    payment_status          bill_payment_status NOT NULL DEFAULT 'PAID',
    status                  bill_status NOT NULL DEFAULT 'COMPLETED',
    idempotency_key         TEXT,
    receipt_url             TEXT,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID REFERENCES users(id),
    updated_by              UUID REFERENCES users(id),
    UNIQUE (tenant_id, bill_no),
    UNIQUE (tenant_id, idempotency_key)
);
CREATE INDEX idx_bills_tenant ON bills(tenant_id, created_at DESC);
CREATE INDEX idx_bills_customer ON bills(customer_id);
CREATE INDEX idx_bills_cashier ON bills(cashier_id);
CREATE INDEX idx_bills_status ON bills(tenant_id, status);

CREATE TABLE bill_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id                 UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    product_id              UUID NOT NULL REFERENCES products(id),
    product_name_snapshot   TEXT NOT NULL,
    sku_snapshot            TEXT NOT NULL,
    hsn_snapshot            TEXT,
    quantity                NUMERIC(12,3) NOT NULL,
    unit_price              NUMERIC(12,2) NOT NULL,
    line_discount           NUMERIC(12,2) NOT NULL DEFAULT 0,
    taxable_value           NUMERIC(12,2) NOT NULL,
    gst_rate                NUMERIC(4,2) NOT NULL,
    cgst_amount             NUMERIC(12,2) NOT NULL DEFAULT 0,
    sgst_amount             NUMERIC(12,2) NOT NULL DEFAULT 0,
    igst_amount             NUMERIC(12,2) NOT NULL DEFAULT 0,
    line_total              NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_bill_items_bill ON bill_items(bill_id);

CREATE TYPE payment_method AS ENUM ('CASH', 'UPI', 'CARD', 'CREDIT', 'SPLIT_COMPONENT');

CREATE TABLE payments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id     UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    method      payment_method NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    tendered    NUMERIC(12,2),
    change_due  NUMERIC(12,2),
    reference   TEXT
);
CREATE INDEX idx_payments_bill ON payments(bill_id);

-- =============================================================
-- CREDIT NOTES (RETURNS)
-- =============================================================
CREATE TABLE credit_notes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    credit_note_no      TEXT NOT NULL,
    original_bill_id    UUID NOT NULL REFERENCES bills(id),
    customer_id         UUID NOT NULL REFERENCES customers(id),
    reason              TEXT,
    subtotal            NUMERIC(12,2) NOT NULL DEFAULT 0,
    cgst_total          NUMERIC(12,2) NOT NULL DEFAULT 0,
    sgst_total          NUMERIC(12,2) NOT NULL DEFAULT 0,
    igst_total          NUMERIC(12,2) NOT NULL DEFAULT 0,
    grand_total         NUMERIC(12,2) NOT NULL DEFAULT 0,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID REFERENCES users(id),
    UNIQUE (tenant_id, credit_note_no)
);
CREATE INDEX idx_credit_notes_tenant ON credit_notes(tenant_id, created_at DESC);

CREATE TABLE credit_note_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_note_id          UUID NOT NULL REFERENCES credit_notes(id) ON DELETE CASCADE,
    product_id              UUID NOT NULL REFERENCES products(id),
    product_name_snapshot   TEXT NOT NULL,
    quantity                NUMERIC(12,3) NOT NULL,
    unit_price              NUMERIC(12,2) NOT NULL,
    taxable_value           NUMERIC(12,2) NOT NULL,
    gst_rate                NUMERIC(4,2) NOT NULL,
    cgst_amount             NUMERIC(12,2) NOT NULL DEFAULT 0,
    sgst_amount             NUMERIC(12,2) NOT NULL DEFAULT 0,
    igst_amount             NUMERIC(12,2) NOT NULL DEFAULT 0,
    line_total              NUMERIC(12,2) NOT NULL
);

-- =============================================================
-- PURCHASES / PROCUREMENT
-- =============================================================
CREATE TYPE purchase_status AS ENUM ('DRAFT', 'PENDING_GRN', 'BILLED', 'PAID');

CREATE TABLE purchases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    purchase_no     TEXT NOT NULL,
    supplier_id     UUID NOT NULL REFERENCES suppliers(id),
    bill_date       DATE NOT NULL,
    due_date        DATE,
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    gst_total       NUMERIC(12,2) NOT NULL DEFAULT 0,
    grand_total     NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          purchase_status NOT NULL DEFAULT 'DRAFT',
    notes           TEXT,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID REFERENCES users(id),
    updated_by      UUID REFERENCES users(id),
    UNIQUE (tenant_id, purchase_no)
);
CREATE INDEX idx_purchases_tenant ON purchases(tenant_id, bill_date DESC);
CREATE INDEX idx_purchases_supplier ON purchases(supplier_id);
CREATE INDEX idx_purchases_status ON purchases(tenant_id, status);

CREATE TABLE purchase_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products(id),
    quantity    NUMERIC(12,3) NOT NULL,
    rate        NUMERIC(12,2) NOT NULL,
    gst_rate    NUMERIC(4,2) NOT NULL DEFAULT 0,
    amount      NUMERIC(12,2) NOT NULL,
    gst_amount  NUMERIC(12,2) NOT NULL DEFAULT 0
);
CREATE INDEX idx_purchase_items_purchase ON purchase_items(purchase_id);

-- =============================================================
-- SETTINGS (per tenant, JSONB sections)
-- =============================================================
CREATE TABLE tenant_settings (
    tenant_id       UUID PRIMARY KEY REFERENCES tenants(id),
    tax             JSONB NOT NULL DEFAULT '{"priceIncludesTax": false, "enabledRates": [0, 5, 12, 18]}',
    payments        JSONB NOT NULL DEFAULT '{"cash": true, "upi": true, "card": false, "credit": true}',
    printer         JSONB NOT NULL DEFAULT '{"widthMm": 80, "autoPrint": false, "copies": 1}',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      UUID REFERENCES users(id)
);

-- =============================================================
-- AUDIT LOG (business events, read-only via API)
-- =============================================================
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID REFERENCES tenants(id),
    user_id         UUID REFERENCES users(id),
    action          TEXT NOT NULL,
    entity_type     TEXT,
    entity_id       UUID,
    metadata        JSONB,
    ip_address      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_tenant ON audit_log(tenant_id, created_at DESC);

-- =============================================================
-- SEED: default walk-in customer (singleton per tenant)
-- Created by app on first tenant setup, not here.
-- =============================================================
