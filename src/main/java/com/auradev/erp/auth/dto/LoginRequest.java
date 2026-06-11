package com.auradev.erp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the {@code POST /api/v1/auth/login} endpoint.
 *
 * @param email    the user's e-mail address — must be a valid, non-blank e-mail
 * @param password the user's plaintext password — must not be blank
 */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {}
