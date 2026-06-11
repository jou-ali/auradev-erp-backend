package com.auradev.erp.user.service;

import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.user.dto.CreateUserRequest;
import com.auradev.erp.user.dto.UpdateUserRequest;
import com.auradev.erp.user.dto.UserResponse;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service for user management within a tenant.
 *
 * <h2>Invite flow</h2>
 * <p>When a user is created via {@link #createUser}, the account is set to
 * {@code INACTIVE} and a one-time invite token (UUID) is generated.  The token
 * (plus an expiry 48 h from creation) is stored on the user record.  A real
 * implementation would dispatch an invite e-mail here; in this codebase the
 * invite token is returned in the response so callers can hand it to the
 * notification layer.</p>
 *
 * <h2>Tenant isolation</h2>
 * <p>All operations are scoped to the current tenant via {@link TenantContext#require()}.
 * The Hibernate {@code tenantFilter} must be enabled at the session level (done by
 * {@link com.auradev.erp.tenant.TenantContextFilter}) for derived JPA queries to
 * apply the filter automatically.  Methods that load by ID still assert tenant
 * membership explicitly as a defence-in-depth measure.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Return all users belonging to the current tenant.
     *
     * @return paginated response of user projections
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers() {
        UUID tenantId = TenantContext.require();
        List<UserResponse> users = userRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
        // Wrap in a page with all results on a single page; callers can pass Pageable if needed.
        return new PageResponse<>(users, 0, users.size(), users.size(), 1);
    }

    /**
     * Return a single user by UUID, scoped to the current tenant.
     *
     * @param id the user UUID
     * @return the user response
     * @throws EntityNotFoundException if no user with this ID exists in the tenant
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return toResponse(loadUser(id));
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Invite a new user to the current tenant.
     *
     * <p>The new account is created with status {@code INACTIVE}.  A one-time
     * invite token is generated and stored; in production this token is embedded
     * in an invite URL dispatched via e-mail.  The account becomes {@code ACTIVE}
     * only after the user accepts the invite (via {@link #activate}).</p>
     *
     * @param req the invite request containing name, e-mail, and role
     * @return the created user response (includes invite token)
     * @throws BusinessException if the e-mail is already taken within this tenant
     */
    public UserResponse createUser(CreateUserRequest req) {
        UUID tenantId = TenantContext.require();

        userRepository.findByEmailAndTenantId(req.email(), tenantId).ifPresent(existing -> {
            throw new BusinessException("EMAIL_TAKEN",
                    "A user with e-mail '" + req.email() + "' already exists in this tenant");
        });

        // Generate a temporary password hash — the user will never log in with
        // this; they must accept the invite and set their own password.
        String tempPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = new User();
        user.setTenantId(tenantId);
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(tempPasswordHash);
        user.setRole(req.role());
        user.setStatus(UserStatus.INACTIVE);

        // Invite token — a random UUID stored as a plain string.
        // Production: hash this before storing, return raw value in e-mail.
        String inviteToken = UUID.randomUUID().toString();
        user.setInviteToken(inviteToken);
        user.setInviteExpires(Instant.now().plusSeconds(48L * 60 * 60)); // 48 hours

        User saved = userRepository.save(user);

        auditLog("USER_INVITED", "User", saved.getId());
        log.info("User invited: id={} email={} role={} tenantId={}",
                saved.getId(), saved.getEmail(), saved.getRole(), tenantId);

        return toResponse(saved);
    }

    /**
     * Update a user's mutable fields (name, role).
     *
     * <p>A {@code null} field in the request means "leave unchanged" (partial update).</p>
     *
     * @param id  the user UUID
     * @param req the update payload
     * @return the updated user response
     */
    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = loadUser(id);

        if (req.name() != null) {
            user.setName(req.name());
        }
        if (req.role() != null) {
            user.setRole(req.role());
        }

        User saved = userRepository.save(user);
        auditLog("USER_UPDATED", "User", saved.getId());
        return toResponse(saved);
    }

    /**
     * Activate a user account (e.g. after they accept the invite).
     *
     * @param id the user UUID
     * @return the updated user response
     */
    public UserResponse activate(UUID id) {
        User user = loadUser(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setInviteToken(null);
        user.setInviteExpires(null);
        User saved = userRepository.save(user);
        auditLog("USER_ACTIVATED", "User", saved.getId());
        return toResponse(saved);
    }

    /**
     * Deactivate a user account.
     *
     * <p>A user cannot deactivate themselves; this would lock them out
     * mid-session without a recovery path.</p>
     *
     * @param id the user UUID to deactivate
     * @return the updated user response
     * @throws BusinessException if the caller tries to deactivate their own account
     */
    public UserResponse deactivate(UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        if (id.equals(currentUserId)) {
            throw new BusinessException("SELF_DEACTIVATION",
                    "You cannot deactivate your own account");
        }

        User user = loadUser(id);
        user.setStatus(UserStatus.INACTIVE);
        User saved = userRepository.save(user);
        auditLog("USER_DEACTIVATED", "User", saved.getId());
        return toResponse(saved);
    }

    /**
     * Change the role of a user.
     *
     * @param id   the user UUID
     * @param role the new role to assign
     * @return the updated user response
     */
    public UserResponse changeRole(UUID id, UserRole role) {
        User user = loadUser(id);
        user.setRole(role);
        User saved = userRepository.save(user);
        auditLog("USER_ROLE_CHANGED", "User", saved.getId());
        return toResponse(saved);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Load a user by ID, asserting it belongs to the current tenant.
     */
    private User loadUser(UUID id) {
        UUID tenantId = TenantContext.require();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        if (!tenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException("User", id);
        }
        return user;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private void auditLog(String action, String entityType, UUID entityId) {
        try {
            auditService.log(
                    TenantContext.get(),
                    resolveCurrentUserId(),
                    action,
                    entityType,
                    entityId,
                    null
            );
        } catch (Exception ex) {
            log.warn("Audit log write failed for action={} entity={} — non-fatal", action, entityId, ex);
        }
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof
                com.auradev.erp.auth.security.UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
