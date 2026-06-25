package com.auradev.erp.audit.controller;

import com.auradev.erp.audit.dto.AuditLogResponse;
import com.auradev.erp.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Tenant audit log")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'AUDIT_VIEW')")
    @Operation(summary = "List recent audit events for the current tenant")
    public ResponseEntity<List<AuditLogResponse>> list(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.listRecent(limit));
    }
}
