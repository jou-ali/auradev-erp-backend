package com.auradev.erp.user.dto;

import com.auradev.erp.auth.security.RolePermissions;
import com.auradev.erp.user.entity.User;

import java.util.List;

public final class UserResponseMapper {

    private UserResponseMapper() {}

    public static UserResponse from(User user) {
        List<String> permissions = RolePermissions.namesFor(user.getRole());
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                permissions);
    }
}
