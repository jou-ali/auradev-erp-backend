package com.auradev.erp.user.controller;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.user.dto.CreateUserRequest;
import com.auradev.erp.user.dto.UpdateUserRequest;
import com.auradev.erp.user.dto.UserResponse;
import com.auradev.erp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Tenant user management")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_USERS_MANAGE')")
    @Operation(summary = "List users in the current tenant")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_USERS_MANAGE')")
    @Operation(summary = "Create a user with email and password")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can(authentication, 'SETTINGS_USERS_MANAGE')")
    @Operation(summary = "Update user name, role, status, or password")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateUser(id, req, principal));
    }
}
