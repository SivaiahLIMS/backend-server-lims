package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oos")
@RequiredArgsConstructor
@Tag(name = "OOS/OOT", description = "Out-of-Specification and Out-of-Trend investigation management")
public class OosController {

    private final DocumentTestResultRepository documentTestResultRepository;
    private final TaskMasterRepository taskMasterRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @Operation(summary = "List all OOS test results")
    public List<DocumentTestResult> getOosResults(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        return documentTestResultRepository.findByTenantIdAndBranchIdAndOosTrue(tenantId, branchId);
    }

    @GetMapping("/worksheet/{worksheetId}")
    @Operation(summary = "Get OOS results for a worksheet")
    public List<DocumentTestResult> getOosByWorksheet(@PathVariable Long worksheetId) {
        return documentTestResultRepository.findByWorksheetExecution_IdAndOosTrue(worksheetId);
    }

    @PostMapping("/{testResultId}/investigate")
    @Operation(summary = "Initiate OOS investigation for a test result")
    public ResponseEntity<DocumentTestResult> investigate(
            @PathVariable Long testResultId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, Object> body) {

        DocumentTestResult result = documentTestResultRepository.findById(testResultId)
                .orElseThrow(() -> LimsException.notFound("Test result not found: " + testResultId));

        Long assigneeId = body.containsKey("assigneeId") ? Long.valueOf(body.get("assigneeId").toString()) : null;
        String description = body.containsKey("description") ? body.get("description").toString() : "OOS Investigation";
        Long requestedById = body.containsKey("requestedById") ? Long.valueOf(body.get("requestedById").toString()) : null;

        AppUser assignee = assigneeId != null ? appUserRepository.findById(assigneeId).orElse(null) : null;
        AppUser requestedBy = requestedById != null ? appUserRepository.findById(requestedById).orElse(null) : null;

        TaskMaster task = TaskMaster.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .type("OOS_INVESTIGATION")
                .status("CREATED")
                .title("OOS Investigation: " + result.getTestName())
                .description(description)
                .refEntity("document_test_result")
                .refId(testResultId)
                .assignee(assignee)
                .createdBy(requestedBy)
                .createdAt(LocalDateTime.now())
                .build();
        task = taskMasterRepository.save(task);

        TaskHistory history = TaskHistory.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .task(task)
                .oldStatus(null)
                .newStatus("CREATED")
                .changedBy(requestedBy)
                .changedAt(LocalDateTime.now())
                .comment("OOS investigation initiated")
                .build();
        taskHistoryRepository.save(history);

        result.setOosInvestigationTask(task);
        result.setOosReason(description);
        result = documentTestResultRepository.save(result);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/tasks/{taskId}/approve")
    @Operation(summary = "Approve OOS investigation task")
    public ResponseEntity<TaskMaster> approveInvestigation(
            @PathVariable Long taskId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, Object> body) {

        TaskMaster task = taskMasterRepository.findById(taskId)
                .orElseThrow(() -> LimsException.notFound("Task not found: " + taskId));

        Long approverId = Long.valueOf(body.get("approverId").toString());
        String comment = body.containsKey("comment") ? body.get("comment").toString() : null;
        AppUser approver = appUserRepository.findById(approverId).orElse(null);

        String old = task.getStatus();
        task.setStatus("APPROVED");
        task.setApprovedBy(approver);
        task.setApprovedAt(LocalDateTime.now());
        task = taskMasterRepository.save(task);

        TaskHistory history = TaskHistory.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .task(task)
                .oldStatus(old)
                .newStatus("APPROVED")
                .changedBy(approver)
                .changedAt(LocalDateTime.now())
                .comment(comment)
                .build();
        taskHistoryRepository.save(history);

        return ResponseEntity.ok(task);
    }
}
