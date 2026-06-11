package com.auradev.erp.auth.security;

import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal wrapping the authenticated user's identity.
 *
 * <p>{@code tenantId} is {@code null} for users with the
 * {@link UserRole#SUPER_ADMIN} role, who operate across all tenants.</p>
 */
@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;

    /** Nullable — {@code null} for SUPER_ADMIN. */
    private final UUID tenantId;

    private final String email;

    private final String passwordHash;

    private final UserRole role;

    private final UserStatus status;

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Build a {@link UserPrincipal} from a persisted {@link User} entity.
     *
     * @param user the JPA entity loaded from the database
     * @return a fully populated principal
     */
    public static UserPrincipal from(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    // -------------------------------------------------------------------------
    // UserDetails contract
    // -------------------------------------------------------------------------

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
