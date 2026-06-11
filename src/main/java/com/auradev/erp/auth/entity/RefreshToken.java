package com.auradev.erp.auth.entity;

import com.auradev.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted (hashed) refresh token used to issue new access tokens.
 *
 * <p>The actual token string is never stored; only a SHA-256 hex digest is
 * kept in {@code tokenHash}.  Tokens are rotated on each use: the old row is
 * marked {@code revoked = true} and a fresh row is inserted.</p>
 *
 * <p>Cascade deletes are handled at the DB level
 * ({@code ON DELETE CASCADE} on the FK to {@code users}).</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user this token belongs to.
     * Deleting a user also deletes all their refresh tokens via DB cascade.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /**
     * SHA-256 hex digest of the opaque token string.
     * The raw token is never persisted.
     */
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    /** When this refresh token becomes invalid regardless of revocation. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /**
     * {@code true} once the token has been used (rotation) or the user has
     * explicitly logged out.
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /** Server-side creation timestamp. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    /** Returns {@code true} if the token is still usable. */
    @Transient
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }
}
