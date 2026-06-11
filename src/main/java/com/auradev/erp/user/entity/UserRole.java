package com.auradev.erp.user.entity;

/**
 * Application roles governing what a user may do within the ERP.
 *
 * <p>SUPER_ADMIN operates across all tenants; every other role is scoped
 * to the user's tenant.</p>
 */
public enum UserRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    MANAGER,
    CASHIER,
    INVENTORY_STAFF,
    ACCOUNTANT
}
