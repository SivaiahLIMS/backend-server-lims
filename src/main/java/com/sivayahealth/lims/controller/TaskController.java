package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.task.TaskActionRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "Unified task engine for all workflow tasks")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "List all tasks for branch",
               description = "Requires: TASK_VIEW. Scoped by X-Branch-Id header. Filter by status param.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TaskMaster>> getTasks(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<TaskMaster> tasks = status != null
                ? taskService.getTasksByStatus(u.getTenantId(), branchId, status)
                : taskService.getTasks(u.getTenantId(), branchId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "Get tasks assigned to current user",
               description = "Requires: TASK_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TaskMaster>> getMyTasks(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(taskService.getMyTasks(u.getUser().getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "Get task by ID",
               description = "Requires: TASK_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<TaskMaster> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE')")
    @Operation(summary = "Create a new task",
               description = "Requires: TASK_CREATE. tenantId and branchId set from JWT and X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> createTask(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody TaskMaster task,
            @AuthenticationPrincipal LimsUserDetails u) {
        task.setTenantId(u.getTenantId());
        task.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(task));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('TASK_ACTION')")
    @Operation(summary = "Accept a task",
               description = "Requires: TASK_ACTION")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Accepted"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> acceptTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body) {
        return ResponseEntity.ok(taskService.acceptTask(id, body.getUserId()));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('TASK_ACTION')")
    @Operation(summary = "Start a task",
               description = "Requires: TASK_ACTION")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Started"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> startTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body) {
        return ResponseEntity.ok(taskService.startTask(id, body.getUserId()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('TASK_ACTION')")
    @Operation(summary = "Complete a task",
               description = "Requires: TASK_ACTION")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Completed"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> completeTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body) {
        return ResponseEntity.ok(taskService.completeTask(id, body.getUserId(), body.getComment()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('TASK_APPROVE')")
    @Operation(summary = "Approve a task",
               description = "Requires: TASK_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> approveTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body) {
        return ResponseEntity.ok(taskService.approveTask(id, body.getUserId(), body.getComment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('TASK_APPROVE')")
    @Operation(summary = "Reject a task",
               description = "Requires: TASK_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rejected"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> rejectTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body) {
        return ResponseEntity.ok(taskService.rejectTask(id, body.getUserId(), body.getComment()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    @Operation(summary = "Get task status history",
               description = "Requires: TASK_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<List<TaskHistory>> getTaskHistory(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskHistory(id));
    }
}
