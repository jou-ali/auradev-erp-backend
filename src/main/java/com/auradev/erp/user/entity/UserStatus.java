package com.auradev.erp.user.entity;

/**
 * Lifecycle status of a {@link User} account.
 *
 * <p>Only {@code ACTIVE} users may authenticate; {@code INACTIVE} accounts
 * are soft-deleted and will be rejected at the login gate.</p>
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}
