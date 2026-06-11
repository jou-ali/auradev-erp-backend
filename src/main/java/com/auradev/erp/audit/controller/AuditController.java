package com.auradev.erp.audit.controller;

import com.auradev.erp.audit.dto.AuditLogResponse;
import com.auradev.erp.audit.entity.AuditLog;
import com.auradev.erp.audit.repository.AuditLogRepository;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the tenant audit log.
 *
 * <p>Access is restricted to {@code TENANT_ADMIN} and {@code SUPER_ADMIN}
 * roles.  Results are automatically scoped to the current tenant via
 * {@link TenantContext}.</p>
 */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    /**
     * Return the paginated audit log for the current tenant, newest entries first.
     *
     * @param pageable pagination parameters (default size = 20)
     * @return paginated list of audit log entries
     */
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID tenantId = TenantContext.require();
        Page<AuditLog> page = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return ResponseEntity.ok(PageResponse.of(page.map(this::toResponse)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AuditLogResponse toResponse(AuditLog log) {
        String userName = (log.getUser() != null) ? log.getUser().getName() : null;
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                userName,
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
