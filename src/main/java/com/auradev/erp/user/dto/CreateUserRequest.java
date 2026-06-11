package com.auradev.erp.user.dto;

import com.auradev.erp.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a new user (admin invite flow).
 *
 * @param name  the user's display name — must not be blank
 * @param email the user's e-mail address — must be a valid e-mail and not blank
 * @param role  the role to assign to the new user — must not be null
 */
public record CreateUserRequest(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email,

        @NotNull
        UserRole role
) {}
