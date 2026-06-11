package com.auradev.erp.auth.repository;

import com.auradev.erp.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Locate a token by its SHA-256 hex digest.
     * Used during token refresh and logout to validate and revoke tokens.
     *
     * @param hash the SHA-256 hex digest of the raw token string
     * @return the matching entity, or empty if not found
     */
    Optional<RefreshToken> findByTokenHash(String hash);

    /**
     * Remove all refresh tokens for a given user.
     * Called when a user's account is deactivated or during a full sign-out.
     *
     * @param userId the user's UUID
     */
    void deleteAllByUserId(UUID userId);
}
