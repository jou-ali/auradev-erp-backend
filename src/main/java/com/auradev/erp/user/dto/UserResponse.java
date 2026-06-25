package com.auradev.erp.user.dto;

import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only projection of a {@code User} entity returned to API clients.
 *
 * <p>Sensitive fields (e.g. {@code passwordHash}, {@code inviteToken}) are
 * deliberately excluded.</p>
 *
 * @param id          the user's UUID
 * @param name        the user's display name
 * @param email       the user's e-mail address
 * @param role        the user's application role
 * @param status      whether the account is ACTIVE or INACTIVE
 * @param lastLoginAt timestamp of the most recent successful login, or {@code null}
 * @param createdAt   when the account was created
 * @param permissions permission names granted to this user's role
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        List<String> permissions
) {}
