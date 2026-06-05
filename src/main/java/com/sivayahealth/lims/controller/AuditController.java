package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.AuditLog;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "Audit log access for compliance — supports filtering, pagination, and date range")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "Search audit trail with optional filters, pagination, and date range")
    public ResponseEntity<Page<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                auditService.searchAuditLogs(u.getTenantId(), entityType, action, userId, from, to, page, size));
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "Get audit trail for a specific entity (chronological order)")
    public ResponseEntity<List<AuditLog>> getEntityAudit(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(auditService.getAuditTrail(u.getTenantId(), entityType, entityId));
    }

    @GetMapping("/meta/entity-types")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "List all distinct entity types that have been audited")
    public ResponseEntity<List<String>> getEntityTypes(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(auditService.getDistinctEntityTypes(u.getTenantId()));
    }

    @GetMapping("/meta/actions")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "List all distinct actions that have been audited")
    public ResponseEntity<List<String>> getActions(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(auditService.getDistinctActions(u.getTenantId()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW')")
    @Operation(summary = "Get audit log statistics for the tenant")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal LimsUserDetails u) {
        long count = auditService.countAuditLogs(u.getTenantId());
        return ResponseEntity.ok(Map.of("totalLogs", count, "tenantId", u.getTenantId()));
    }
}
