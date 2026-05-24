package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Unified task engine for all workflow tasks")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "List all tasks")
    public List<TaskMaster> getTasks(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String status) {
        return status != null
                ? taskService.getTasksByStatus(tenantId, branchId, status)
                : taskService.getTasks(tenantId, branchId);
    }

    @GetMapping("/my")
    @Operation(summary = "Get tasks assigned to current user")
    public List<TaskMaster> getMyTasks(@RequestParam Long userId) {
        return taskService.getMyTasks(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public TaskMaster getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskMaster> createTask(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody TaskMaster task) {
        task.setTenantId(tenantId);
        task.setBranchId(branchId);
        return ResponseEntity.status(201).body(taskService.createTask(task));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a task")
    public TaskMaster acceptTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        return taskService.acceptTask(id, userId);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a task")
    public TaskMaster startTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        return taskService.startTask(id, userId);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a task")
    public TaskMaster completeTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String comment = body.containsKey("comment") ? body.get("comment").toString() : null;
        return taskService.completeTask(id, userId, comment);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a task")
    public TaskMaster approveTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String comment = body.containsKey("comment") ? body.get("comment").toString() : null;
        return taskService.approveTask(id, userId, comment);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a task")
    public TaskMaster rejectTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String comment = body.containsKey("comment") ? body.get("comment").toString() : null;
        return taskService.rejectTask(id, userId, comment);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get task status history")
    public List<TaskHistory> getTaskHistory(@PathVariable Long id) {
        return taskService.getTaskHistory(id);
    }
}
