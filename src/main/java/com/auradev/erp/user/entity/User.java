package com.auradev.erp.user.entity;

import com.auradev.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted user account.
 *
 * <p>The Hibernate tenant filter ({@code tenantFilter}) is enabled at the
 * repository / service layer for every request so that queries are automatically
 * scoped to the current tenant.  SUPER_ADMIN accounts carry a {@code null}
 * {@code tenantId} and must bypass the filter.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "users")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    /** One-time token sent in the invitation e-mail. */
    @Column(name = "invite_token")
    private String inviteToken;

    /** When the invitation token expires. */
    @Column(name = "invite_expires")
    private Instant inviteExpires;

    /** Timestamp of the user's most recent successful login. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
