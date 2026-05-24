package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.EmailLog;
import com.sivayahealth.lims.entity.Notification;
import com.sivayahealth.lims.repository.EmailLogRepository;
import com.sivayahealth.lims.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications and email log management")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;

    @GetMapping
    @Operation(summary = "Get notifications for a user")
    public List<Notification> getNotifications(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return notificationRepository.findByTenantIdAndUserId(tenantId, userId);
        }
        return notificationRepository.findByTenantId(tenantId);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        return notificationRepository.findById(id).map(n -> {
            n.setStatus("READ");
            return ResponseEntity.ok(notificationRepository.save(n));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/emails")
    @Operation(summary = "Get email dispatch log")
    public List<EmailLog> getEmailLog(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-Branch-Id") Long branchId) {
        return emailLogRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @GetMapping("/settings")
    @Operation(summary = "Get notification settings (placeholder)")
    public ResponseEntity<Object> getNotificationSettings(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        return ResponseEntity.ok(java.util.Map.of("tenantId", tenantId, "emailEnabled", true, "pushEnabled", false));
    }
}
