package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.oos.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/oos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "OOS/OOT", description = "Out-of-Specification and Out-of-Trend investigation management")
public class OosController {

    private final DocumentTestResultRepository documentTestResultRepository;
    private final TaskMasterRepository taskMasterRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "List all OOS test results for branch",
               description = "Requires: OOS_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<DocumentTestResult>> getOosResults(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                documentTestResultRepository.findByTenantIdAndBranchIdAndOosTrue(u.getTenantId(), branchId)
        );
    }

    @GetMapping("/worksheet/{worksheetId}")
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "Get OOS results for a worksheet",
               description = "Requires: OOS_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<List<DocumentTestResult>> getOosByWorksheet(@PathVariable Long worksheetId) {
        return ResponseEntity.ok(
                documentTestResultRepository.findByWorksheetExecution_IdAndOosTrue(worksheetId)
        );
    }

    @PostMapping("/{testResultId}/investigate")
    @PreAuthorize("hasAuthority('OOS_INVESTIGATE')")
    @Operation(summary = "Initiate OOS investigation for a test result",
               description = "Requires: OOS_INVESTIGATE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Investigation initiated"),
        @ApiResponse(responseCode = "404", description = "Test result not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<DocumentTestResult> investigate(
            @PathVariable Long testResultId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody InitiateOosInvestigationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        DocumentTestResult result = documentTestResultRepository.findById(testResultId)
                .orElseThrow(() -> LimsException.notFound("Test result not found: " + testResultId));

        AppUser assignee = body.getAssigneeId() != null
                ? appUserRepository.findById(body.getAssigneeId()).orElse(null) : null;
        AppUser requestedBy = body.getRequestedById() != null
                ? appUserRepository.findById(body.getRequestedById()).orElse(null) : null;
        String description = body.getDescription() != null ? body.getDescription() : "OOS Investigation";

        TaskMaster task = TaskMaster.builder()
                .tenantId(u.getTenantId())
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
                .tenantId(u.getTenantId())
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
        return ResponseEntity.ok(documentTestResultRepository.save(result));
    }

    @PostMapping("/tasks/{taskId}/approve")
    @PreAuthorize("hasAuthority('OOS_APPROVE')")
    @Operation(summary = "Approve OOS investigation task",
               description = "Requires: OOS_APPROVE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TaskMaster> approveInvestigation(
            @PathVariable Long taskId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ApproveOosInvestigationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        TaskMaster task = taskMasterRepository.findById(taskId)
                .orElseThrow(() -> LimsException.notFound("Task not found: " + taskId));

        AppUser approver = appUserRepository.findById(body.getApproverId()).orElse(null);
        String old = task.getStatus();
        task.setStatus("APPROVED");
        task.setApprovedBy(approver);
        task.setApprovedAt(LocalDateTime.now());
        task = taskMasterRepository.save(task);

        TaskHistory history = TaskHistory.builder()
                .tenantId(u.getTenantId())
                .branchId(branchId)
                .task(task)
                .oldStatus(old)
                .newStatus("APPROVED")
                .changedBy(approver)
                .changedAt(LocalDateTime.now())
                .comment(body.getComment())
                .build();
        taskHistoryRepository.save(history);

        return ResponseEntity.ok(task);
    }
}
