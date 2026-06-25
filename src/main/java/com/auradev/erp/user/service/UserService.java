package com.auradev.erp.user.service;

import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.user.dto.CreateUserRequest;
import com.auradev.erp.user.dto.UpdateUserRequest;
import com.auradev.erp.user.dto.UserResponse;
import com.auradev.erp.user.dto.UserResponseMapper;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        UUID tenantId = TenantContext.require();
        return userRepository.findAllByTenantId(tenantId).stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest req) {
        assertAssignableRole(req.role());

        UUID tenantId = TenantContext.require();
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("EMAIL_IN_USE", "A user with this email already exists");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setRole(req.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        User saved = userRepository.save(user);
        auditService.log("USER_CREATED", "user", saved.getId(), Map.of(
                "email", saved.getEmail(),
                "role", saved.getRole().name()));
        return toResponse(saved);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest req, UserPrincipal actor) {
        User user = loadTenantUser(id);

        if (req.name() != null && !req.name().isBlank()) {
            user.setName(req.name().trim());
        }
        if (req.role() != null) {
            assertAssignableRole(req.role());
            if (user.getId().equals(actor.getId()) && req.role() != user.getRole()) {
                throw new BusinessException("SELF_ROLE", "You cannot change your own role");
            }
            user.setRole(req.role());
        }
        if (req.status() != null) {
            if (user.getId().equals(actor.getId()) && req.status() == UserStatus.INACTIVE) {
                throw new BusinessException("SELF_DEACTIVATE", "You cannot deactivate your own account");
            }
            user.setStatus(req.status());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }

        User saved = userRepository.save(user);
        auditService.log("USER_UPDATED", "user", saved.getId(), Map.of(
                "email", saved.getEmail(),
                "role", saved.getRole().name(),
                "status", saved.getStatus().name()));
        return toResponse(saved);
    }

    private User loadTenantUser(UUID id) {
        UUID tenantId = TenantContext.require();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        if (!tenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException("User", id);
        }
        return user;
    }

    private void assertAssignableRole(UserRole role) {
        if (role == UserRole.SUPER_ADMIN) {
            throw new BusinessException("INVALID_ROLE", "Super Admin accounts cannot be created from tenant settings");
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponseMapper.from(user);
    }
}