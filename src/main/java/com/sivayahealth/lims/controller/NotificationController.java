package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.EmailLog;
import com.sivayahealth.lims.entity.Notification;
import com.sivayahealth.lims.repository.EmailLogRepository;
import com.sivayahealth.lims.repository.NotificationRepository;
import com.sivayahealth.lims.security.LimsUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "User notifications and email log management")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get notifications for current user or entire tenant",
               description = "Requires: NOTIFICATION_VIEW. Pass userId param to filter to a specific user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<Notification> notifications = userId != null
                ? notificationRepository.findByTenantIdAndUserId(u.getTenantId(), userId)
                : notificationRepository.findByTenantId(u.getTenantId());
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Mark notification as read",
               description = "Requires: NOTIFICATION_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Marked as read"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        return notificationRepository.findById(id).map(n -> {
            n.setStatus("READ");
            return ResponseEntity.ok(notificationRepository.save(n));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/emails")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get email dispatch log for branch",
               description = "Requires: NOTIFICATION_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<EmailLog>> getEmailLog(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(emailLogRepository.findByTenantIdAndBranchId(u.getTenantId(), branchId));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    @Operation(summary = "Get notification settings",
               description = "Requires: NOTIFICATION_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getNotificationSettings(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                Map.of("tenantId", u.getTenantId(), "emailEnabled", true, "pushEnabled", false)
        );
    }
}
