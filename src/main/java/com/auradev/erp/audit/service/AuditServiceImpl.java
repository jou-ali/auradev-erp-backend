package com.auradev.erp.audit.service;

import com.auradev.erp.audit.entity.AuditLog;
import com.auradev.erp.audit.repository.AuditLogRepository;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Asynchronous implementation of {@link AuditService}.
 *
 * <p>Each call executes in a separate thread (Spring's {@code asyncExecutor}
 * thread pool) and within an independent transaction ({@code REQUIRES_NEW}) so
 * that an audit failure never rolls back the surrounding business transaction.</p>
 *
 * <p>All exceptions are caught internally; a failed audit entry is logged at
 * ERROR level but never re-thrown to the caller.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     *
     * <p>This method runs asynchronously in the {@code asyncExecutor} thread pool
     * and commits in its own transaction.  Any exception is swallowed and logged.</p>
     */
    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void log(UUID tenantId, UUID userId, String action,
                    String entityType, UUID entityId, String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTenantId(tenantId);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setIpAddress(ipAddress);
            entry.setCreatedAt(Instant.now());

            if (userId != null) {
                userRepository.findById(userId).ifPresent(entry::setUser);
            }

            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to write audit log entry [action={}, tenantId={}, entityType={}, entityId={}]: {}",
                    action, tenantId, entityType, entityId, ex.getMessage(), ex);
        }
    }
}
