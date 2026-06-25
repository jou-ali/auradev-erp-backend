package com.auradev.erp.auth.security;

import com.auradev.erp.user.entity.UserRole;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Maps each {@link UserRole} to its {@link Permission} set.
 * Keep {@code lib/permissions.ts} in sync (Permission names only).
 */
public final class RolePermissions {

    private RolePermissions() {}

    private static final Set<Permission> ALL = EnumSet.allOf(Permission.class);

    private static final Set<Permission> TENANT_ADMIN_PERMS = ALL;

    private static final Set<Permission> SUPER_ADMIN_PERMS = ALL;

    private static final Set<Permission> MANAGER_PERMS = EnumSet.of(
            Permission.DASHBOARD_VIEW,
            Permission.BILL_CREATE,
            Permission.BILL_VIEW,
            Permission.BILL_HOLD,
            Permission.INVENTORY_VIEW,
            Permission.INVENTORY_EDIT,
            Permission.INVENTORY_IMPORT,
            Permission.INVENTORY_EXPORT,
            Permission.PRODUCT_MANAGE,
            Permission.PURCHASE_VIEW,
            Permission.PURCHASE_MANAGE,
            Permission.SUPPLIER_MANAGE,
            Permission.CUSTOMER_VIEW,
            Permission.SETTINGS_STORE_VIEW,
            Permission.SETTINGS_BILLING_VIEW,
            Permission.SETTINGS_BILLING_EDIT,
            Permission.SETTINGS_PRINTER_VIEW,
            Permission.SETTINGS_PRINTER_EDIT,
            Permission.AUDIT_VIEW
    );

    private static final Set<Permission> CASHIER_PERMS = EnumSet.of(
            Permission.DASHBOARD_VIEW,
            Permission.BILL_CREATE,
            Permission.BILL_VIEW,
            Permission.BILL_HOLD,
            Permission.INVENTORY_VIEW,
            Permission.CUSTOMER_VIEW,
            Permission.SETTINGS_PRINTER_VIEW
    );

    private static final Set<Permission> INVENTORY_STAFF_PERMS = EnumSet.of(
            Permission.DASHBOARD_VIEW,
            Permission.INVENTORY_VIEW,
            Permission.INVENTORY_EDIT,
            Permission.INVENTORY_IMPORT,
            Permission.PURCHASE_VIEW,
            Permission.PURCHASE_MANAGE,
            Permission.SUPPLIER_MANAGE
    );

    private static final Set<Permission> ACCOUNTANT_PERMS = EnumSet.of(
            Permission.DASHBOARD_VIEW,
            Permission.BILL_VIEW,
            Permission.INVENTORY_VIEW,
            Permission.INVENTORY_EXPORT,
            Permission.PURCHASE_VIEW,
            Permission.AUDIT_VIEW
    );

    public static Set<Permission> forRole(UserRole role) {
        if (role == null) return Set.of();
        return switch (role) {
            case SUPER_ADMIN -> SUPER_ADMIN_PERMS;
            case TENANT_ADMIN -> TENANT_ADMIN_PERMS;
            case MANAGER -> MANAGER_PERMS;
            case CASHIER -> CASHIER_PERMS;
            case INVENTORY_STAFF -> INVENTORY_STAFF_PERMS;
            case ACCOUNTANT -> ACCOUNTANT_PERMS;
        };
    }

    public static boolean has(UserRole role, Permission permission) {
        return forRole(role).contains(permission);
    }

    public static List<String> namesFor(UserRole role) {
        return forRole(role).stream()
                .map(Permission::name)
                .sorted()
                .toList();
    }
}
