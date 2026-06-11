package com.auradev.erp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stateless JWT utility service.
 *
 * <p>Access tokens are short-lived; refresh tokens use a longer expiry.
 * Both are signed with HMAC-SHA256 using the configured secret.</p>
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_USER_ID  = "userId";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLE      = "role";

    private final SecretKey signingKey;
    private final long accessExpiry;
    private final long refreshExpiry;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessExpiry,
            @Value("${app.jwt.refresh-token-expiry-ms}") long refreshExpiry) {
        this.signingKey   = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiry  = accessExpiry;
        this.refreshExpiry = refreshExpiry;
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generate a short-lived access token.
     *
     * @param principal the authenticated principal
     * @return signed JWT string
     */
    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, accessExpiry);
    }

    /**
     * Generate a long-lived refresh token.
     *
     * @param principal the authenticated principal
     * @return signed JWT string
     */
    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, refreshExpiry);
    }

    // -------------------------------------------------------------------------
    // Claims extraction
    // -------------------------------------------------------------------------

    /**
     * Parse and verify the JWT, returning all claims.
     *
     * @param token the compact JWT string
     * @return verified {@link Claims}
     * @throws JwtException if the token is malformed, expired, or signature invalid
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the {@code sub} (email) claim.
     *
     * @param token the compact JWT string
     * @return the subject email
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract the {@code userId} claim as a {@link UUID}.
     *
     * @param token the compact JWT string
     * @return the user UUID
     */
    public UUID extractUserId(String token) {
        String raw = extractAllClaims(token).get(CLAIM_USER_ID, String.class);
        return UUID.fromString(raw);
    }

    /**
     * Extract the {@code tenantId} claim as a {@link UUID}.
     *
     * @param token the compact JWT string
     * @return the tenant UUID, or {@code null} if the claim is absent (SUPER_ADMIN)
     */
    public UUID extractTenantId(String token) {
        String raw = extractAllClaims(token).get(CLAIM_TENANT_ID, String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }

    /**
     * Extract the {@code role} claim.
     *
     * @param token the compact JWT string
     * @return the role name string
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get(CLAIM_ROLE, String.class);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Return {@code true} if the token parses correctly and has not expired.
     *
     * @param token the compact JWT string
     * @return {@code true} when valid
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildToken(UserPrincipal principal, long expiryMs) {
        long now = System.currentTimeMillis();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(CLAIM_USER_ID, principal.getId().toString());
        extraClaims.put(CLAIM_ROLE, principal.getRole().name());
        if (principal.getTenantId() != null) {
            extraClaims.put(CLAIM_TENANT_ID, principal.getTenantId().toString());
        }

        return Jwts.builder()
                .claims(extraClaims)
                .subject(principal.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
