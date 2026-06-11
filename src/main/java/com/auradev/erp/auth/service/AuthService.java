package com.auradev.erp.auth.service;

import com.auradev.erp.auth.dto.LoginRequest;
import com.auradev.erp.auth.dto.LoginResponse;
import com.auradev.erp.auth.dto.RefreshRequest;
import com.auradev.erp.auth.entity.RefreshToken;
import com.auradev.erp.auth.repository.RefreshTokenRepository;
import com.auradev.erp.auth.security.JwtService;
import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.user.dto.UserResponse;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserStatus;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Handles user authentication: login, access-token refresh, and logout.
 *
 * <h2>Refresh-token security model</h2>
 * <ul>
 *   <li>The raw token string is returned to the caller but never stored.</li>
 *   <li>A SHA-256 hex digest of the token is persisted in {@code refresh_tokens}.</li>
 *   <li>Tokens are rotated on each use: the old row is marked {@code revoked = true}
 *       and a new row is inserted.</li>
 *   <li>An attempt to use an already-revoked token is treated as a potential
 *       replay attack and results in an HTTP 401.</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository          userRepository;
    private final JwtService              jwtService;
    private final PasswordEncoder         passwordEncoder;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final AuditService            auditService;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshExpiryMs;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessExpiryMs;

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticate a user with e-mail + password.
     *
     * <p>Steps:
     * <ol>
     *   <li>Look up the user by e-mail (bypasses tenant filter — needed for
     *       cross-tenant SUPER_ADMIN and for users before the tenant context
     *       is established).</li>
     *   <li>Reject if not found or account is INACTIVE.</li>
     *   <li>Verify the BCrypt password hash.</li>
     *   <li>Generate a new access token and refresh token.</li>
     *   <li>Persist a hashed copy of the refresh token.</li>
     *   <li>Update {@code last_login_at}.</li>
     *   <li>Write an audit log entry.</li>
     * </ol>
     * </p>
     *
     * @param req       the login credentials
     * @param ipAddress the originating request IP (for audit logging)
     * @return a {@link LoginResponse} containing both tokens and the user profile
     * @throws ResponseStatusException HTTP 401 if credentials are invalid or
     *                                 the account is inactive
     */
    public LoginResponse login(LoginRequest req, String ipAddress) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> unauthorised("Invalid credentials"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw unauthorised("Account is inactive");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw unauthorised("Invalid credentials");
        }

        UserPrincipal principal = UserPrincipal.from(user);

        String accessToken  = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        persistRefreshToken(user, refreshToken);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.log(
                user.getTenantId(),
                user.getId(),
                "LOGIN_SUCCESS",
                "User",
                user.getId(),
                ipAddress
        );

        return buildLoginResponse(accessToken, refreshToken, user);
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    /**
     * Exchange a valid refresh token for a new access + refresh token pair
     * (token rotation).
     *
     * @param req the refresh-token request body
     * @return a new {@link LoginResponse}
     * @throws ResponseStatusException HTTP 401 if the token is unknown, revoked,
     *                                 or expired
     */
    public LoginResponse refresh(RefreshRequest req) {
        String hash = sha256Hex(req.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> unauthorised("Invalid refresh token"));

        if (!stored.isValid()) {
            throw unauthorised("Refresh token is expired or revoked");
        }

        // Revoke the consumed token (rotation).
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        UserPrincipal principal = UserPrincipal.from(user);

        String newAccessToken  = jwtService.generateAccessToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);

        persistRefreshToken(user, newRefreshToken);

        return buildLoginResponse(newAccessToken, newRefreshToken, user);
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    /**
     * Revoke the given refresh token, effectively ending the session.
     *
     * <p>No-op if the token is not found (already expired / deleted).</p>
     *
     * @param rawRefreshToken the opaque refresh token string
     */
    public void logout(String rawRefreshToken) {
        String hash = sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void persistRefreshToken(User user, String rawToken) {
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plusMillis(refreshExpiryMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(entity);
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, User user) {
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
        return new LoginResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                accessExpiryMs / 1000L,
                userResponse
        );
    }

    /**
     * Compute the SHA-256 hex digest of {@code input}.
     * Used to derive the token hash stored in the database.
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec; this branch is unreachable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static ResponseStatusException unauthorised(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
