package com.auradev.erp.auth.security;

/**
 * Application permissions — single source of truth for RBAC.
 * Assigned to roles via {@link RolePermissions}.
 */
public enum Permission {
    DASHBOARD_VIEW,

    BILL_CREATE,
    BILL_VIEW,
    BILL_HOLD,

    INVENTORY_VIEW,
    INVENTORY_EDIT,
    INVENTORY_IMPORT,
    INVENTORY_EXPORT,

    PRODUCT_MANAGE,

    PURCHASE_VIEW,
    PURCHASE_MANAGE,

    SUPPLIER_MANAGE,

    CUSTOMER_VIEW,

    SETTINGS_STORE_VIEW,
    SETTINGS_STORE_EDIT,
    SETTINGS_BILLING_VIEW,
    SETTINGS_BILLING_EDIT,
    SETTINGS_PRINTER_VIEW,
    SETTINGS_PRINTER_EDIT,

    SETTINGS_USERS_MANAGE,
    AUDIT_VIEW
}
