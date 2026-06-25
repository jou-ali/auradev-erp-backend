package com.auradev.erp.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component("authz")
public class PermissionChecker {

    public boolean can(Authentication authentication, String permission) {
        return resolve(authentication)
                .map(p -> RolePermissions.has(p.getRole(), Permission.valueOf(permission)))
                .orElse(false);
    }

    public boolean canAny(Authentication authentication, String... permissions) {
        return resolve(authentication)
                .map(p -> Arrays.stream(permissions)
                        .anyMatch(perm -> RolePermissions.has(p.getRole(), Permission.valueOf(perm))))
                .orElse(false);
    }

    private static Optional<UserPrincipal> resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }
}
