package com.auradev.erp.audit.service;

import com.auradev.erp.audit.dto.AuditLogResponse;
import com.auradev.erp.audit.entity.AuditLog;
import com.auradev.erp.audit.repository.AuditLogRepository;
import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.common.web.RequestIpHelper;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        UUID tenantId = TenantContext.get();
        UUID userId = currentUserId().orElse(null);
        persist(tenantId, userId, action, entityType, entityId, metadata, currentIp());
    }

    public void logForUser(User user, String action, String entityType, UUID entityId, Map<String, Object> metadata, String ip) {
        persist(user.getTenantId(), user.getId(), action, entityType, entityId, metadata, ip);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listRecent(int limit) {
        UUID tenantId = TenantContext.require();
        int size = Math.min(Math.max(limit, 1), 100);
        Page<AuditLog> page = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(
                tenantId, PageRequest.of(0, size));

        Set<UUID> userIds = page.getContent().stream()
                .map(AuditLog::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<UUID, String> names = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return page.getContent().stream()
                .map(entry -> toResponse(entry, names))
                .toList();
    }

    private void persist(
            UUID tenantId,
            UUID userId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> metadata,
            String ip) {
        AuditLog row = new AuditLog();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setAction(action);
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        row.setMetadata(metadata);
        row.setIpAddress(ip);
        auditLogRepository.save(row);
    }

    private AuditLogResponse toResponse(AuditLog entry, Map<UUID, String> names) {
        String userName = entry.getUserId() == null
                ? "System"
                : names.getOrDefault(entry.getUserId(), "Unknown user");
        return new AuditLogResponse(
                entry.getId(),
                entry.getAction(),
                userName,
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getMetadata(),
                entry.getIpAddress(),
                entry.getCreatedAt());
    }

    private static Optional<UUID> currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getId());
        }
        return Optional.empty();
    }

    private static String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return RequestIpHelper.clientIp(request);
        }
        return null;
    }
}
