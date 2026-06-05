package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.task.CreateScheduledTaskRequest;
import com.sivayahealth.lims.dto.task.UpdateTaskStatusRequest;
import com.sivayahealth.lims.entity.ScheduledTask;
import com.sivayahealth.lims.entity.TaskStatusEnum;
import com.sivayahealth.lims.entity.TaskType;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Scheduled Tasks", description = "Scheduled task management and execution engine")
public class ScheduledTaskController {

    private final ScheduledTaskService scheduledTaskService;

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE')")
    @Operation(summary = "Create a scheduled task")
    public ResponseEntity<ScheduledTask> createTask(
            @RequestBody CreateScheduledTaskRequest body,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                scheduledTaskService.createTask(
                        u.getTenantId(), branchId, body.getTaskType(), body.getTitle(),
                        body.getDescription(), body.getDueDate(), body.getRecurrenceRule(),
                        body.getAssignedToUserId(), body.getRefEntity(), body.getRefId(),
                        u.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "List tasks — optionally filter by status or type")
    public ResponseEntity<List<ScheduledTask>> getTasks(
            @RequestParam(required = false) TaskStatusEnum status,
            @RequestParam(required = false) TaskType taskType,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        if (status != null) {
            return ResponseEntity.ok(scheduledTaskService.getTasksByStatus(u.getTenantId(), branchId, status));
        }
        if (taskType != null) {
            return ResponseEntity.ok(scheduledTaskService.getTasksByType(u.getTenantId(), branchId, taskType));
        }
        return ResponseEntity.ok(scheduledTaskService.getTasks(u.getTenantId(), branchId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "Get tasks assigned to the current user")
    public ResponseEntity<List<ScheduledTask>> getMyTasks(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(scheduledTaskService.getMyTasks(u.getUser().getId()));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "Get overdue pending tasks for tenant")
    public ResponseEntity<List<ScheduledTask>> getOverdueTasks(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(scheduledTaskService.getOverdueTasks(u.getTenantId()));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "Get task details")
    public ResponseEntity<ScheduledTask> getTaskById(
            @PathVariable Long taskId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(scheduledTaskService.getTaskById(taskId));
    }

    @PostMapping("/{taskId}/status")
    @PreAuthorize("hasAuthority('TASK_EDIT')")
    @Operation(summary = "Update task status")
    public ResponseEntity<ScheduledTask> updateStatus(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskStatusRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                scheduledTaskService.updateStatus(taskId, body.getStatus(), body.getResultNotes(), u.getUser().getId()));
    }
}
