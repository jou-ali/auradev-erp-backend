package com.auradev.erp.user.dto;

import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 120) String name,
        UserRole role,
        UserStatus status,
        @Size(min = 8, max = 72) String password
) {}
