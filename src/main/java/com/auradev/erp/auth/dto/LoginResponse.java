package com.auradev.erp.auth.dto;

import com.auradev.erp.user.dto.UserResponse;

/**
 * Response body returned by {@code POST /api/v1/auth/login} and
 * {@code POST /api/v1/auth/refresh}.
 *
 * @param accessToken  short-lived JWT access token
 * @param refreshToken long-lived opaque refresh token (store in httpOnly cookie
 *                     or secure storage — never in localStorage)
 * @param tokenType    always {@code "Bearer"}
 * @param expiresIn    access-token lifetime in seconds
 * @param user         public profile of the authenticated user
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
