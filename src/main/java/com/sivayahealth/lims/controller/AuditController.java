package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.AuditLog;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "Audit log access for compliance")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "Get full audit trail for tenant")
    public ResponseEntity<List<AuditLog>> getAuditTrail(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(auditService.getTenantAuditTrail(u.getTenantId()));
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "Get audit trail for a specific entity")
    public ResponseEntity<List<AuditLog>> getEntityAudit(@PathVariable String entityType,
                                                          @PathVariable Long entityId,
                                                          @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(auditService.getAuditTrail(u.getTenantId(), entityType, entityId));
    }
}
