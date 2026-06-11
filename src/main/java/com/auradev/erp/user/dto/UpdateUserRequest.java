package com.auradev.erp.user.dto;

import com.auradev.erp.user.entity.UserRole;

/**
 * Request body for patching an existing user.
 *
 * <p>All fields are nullable — a {@code null} value means "leave unchanged".</p>
 *
 * @param name the new display name, or {@code null} to keep the current value
 * @param role the new role, or {@code null} to keep the current value
 */
public record UpdateUserRequest(
        String name,
        UserRole role
) {}
