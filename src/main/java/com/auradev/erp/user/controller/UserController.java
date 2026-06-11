package com.auradev.erp.user.controller;

import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.user.dto.CreateUserRequest;
import com.auradev.erp.user.dto.UpdateUserRequest;
import com.auradev.erp.user.dto.UserResponse;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for tenant-scoped user management.
 *
 * <p>All endpoints are restricted to users with {@code TENANT_ADMIN} or
 * {@code SUPER_ADMIN} roles.  Tenant scoping is enforced by
 * {@link UserService} using {@link com.auradev.erp.tenant.TenantContext}.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Users", description = "Tenant user management — invite, activate, deactivate, and role assignment")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =========================================================================
    // List users
    // =========================================================================

    @Operation(summary = "List all users in the current tenant")
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    // =========================================================================
    // Create / invite user
    // =========================================================================

    @Operation(
        summary = "Invite a new user",
        description = "Creates a new INACTIVE user with an invite token.  "
                + "The caller is responsible for sending the invite e-mail."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    // =========================================================================
    // Get user
    // =========================================================================

    @Operation(summary = "Get a user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {

        return ResponseEntity.ok(userService.getUser(id));
    }

    // =========================================================================
    // Update user
    // =========================================================================

    @Operation(summary = "Update a user's name or role (partial update — null fields are ignored)")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @RequestBody UpdateUserRequest req) {

        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    // =========================================================================
    // Activate / deactivate
    // =========================================================================

    @Operation(summary = "Activate a user account")
    @PostMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activate(
            @Parameter(description = "User UUID") @PathVariable UUID id) {

        return ResponseEntity.ok(userService.activate(id));
    }

    @Operation(
        summary = "Deactivate a user account",
        description = "The authenticated user cannot deactivate their own account."
    )
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(
            @Parameter(description = "User UUID") @PathVariable UUID id) {

        return ResponseEntity.ok(userService.deactivate(id));
    }

    // =========================================================================
    // Role change
    // =========================================================================

    @Operation(summary = "Change a user's role")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Parameter(description = "New role") @RequestParam UserRole role) {

        return ResponseEntity.ok(userService.changeRole(id, role));
    }
}
