package com.auradev.erp.audit;

import com.auradev.erp.audit.entity.AuditLog;
import com.auradev.erp.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditLog(AuditLogEvent event) {
        try {
            AuditLog row = new AuditLog();
            row.setTenantId(event.tenantId());
            row.setUserId(event.userId());
            row.setAction(event.action());
            row.setEntityType(event.entityType());
            row.setEntityId(event.entityId());
            row.setMetadata(event.metadata());
            row.setIpAddress(event.ip());
            auditLogRepository.save(row);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log action={} entity={}: {}",
                    event.action(), event.entityType(), ex.getMessage());
        }
    }
}
