package com.auradev.erp.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the {@code POST /api/v1/auth/refresh} endpoint.
 *
 * @param refreshToken the opaque refresh token issued at login — must not be blank
 */
public record RefreshRequest(

        @NotBlank
        String refreshToken
) {}
