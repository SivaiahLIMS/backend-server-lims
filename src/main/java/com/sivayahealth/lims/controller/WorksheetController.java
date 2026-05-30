package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.WorksheetDocumentService;
import com.sivayahealth.lims.service.WorksheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worksheets")
@RequiredArgsConstructor
@Tag(name = "Worksheet Management",
     description = "Worksheet lifecycle, execution data, and review history. " +
                   "ALL endpoints require branchId query param + JWT tenant scope.")
public class WorksheetController {

    private final WorksheetService         worksheetService;
    private final WorksheetDocumentService  worksheetDocumentService;

    // ── List / Search ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List all worksheets for tenant + branch")
    public ResponseEntity<List<WorksheetMaster>> listAll(
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listAll(u.getTenantId(), branchId));
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List worksheets filtered by status",
               description = "Valid statuses: DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, CLOSED")
    public ResponseEntity<List<WorksheetMaster>> listByStatus(
            @RequestParam Long branchId,
            @RequestParam String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listByStatus(u.getTenantId(), branchId, status));
    }

    @GetMapping("/archived")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List archived worksheets for tenant + branch")
    public ResponseEntity<List<WorksheetMaster>> listArchived(
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listArchived(u.getTenantId(), branchId));
    }

    @GetMapping("/assigned-to/{userId}")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List worksheets assigned to a specific user")
    public ResponseEntity<List<WorksheetMaster>> listAssignedTo(
            @PathVariable Long userId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listAssignedTo(u.getTenantId(), branchId, userId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Search worksheet history with optional filters",
               description = "All filters optional: status, isArchived, productId, assignedToId, batchNo, from (ISO datetime), to (ISO datetime).")
    public ResponseEntity<List<WorksheetMaster>> search(
            @RequestParam Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.search(
                u.getTenantId(), branchId, status, isArchived,
                productId, assignedToId, batchNo, from, to));
    }

    @GetMapping("/{worksheetId}")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get a single worksheet by ID (tenant + branch scoped)")
    public ResponseEntity<WorksheetMaster> getById(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.getById(u.getTenantId(), branchId, worksheetId));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('WORKSHEET_CREATE')")
    @Operation(summary = "Create a new worksheet (status starts as DRAFT)",
               description = "Optional body fields: batchNo, productId, templateId.")
    public ResponseEntity<WorksheetMaster> create(
            @RequestBody WorksheetMaster body,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        body.setTenant(null);
        body.setBranch(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(worksheetService.create(u.getTenantId(), branchId, u.getUser().getId(), body));
    }

    @PutMapping("/{worksheetId}")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Update worksheet fields (only DRAFT or REJECTED)",
               description = "Editable fields: batchNo, productId, templateId.")
    public ResponseEntity<WorksheetMaster> update(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody Map<String, Object> fields,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.update(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), fields));
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/assign")
    @PreAuthorize("hasAuthority('WORKSHEET_ASSIGN')")
    @Operation(summary = "Assign a worksheet to an analyst",
               description = "Body: assignToUserId (Long).")
    public ResponseEntity<WorksheetMaster> assign(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        Long assignToUserId = ((Number) body.get("assignToUserId")).longValue();
        return ResponseEntity.ok(
                worksheetService.assign(u.getTenantId(), branchId, worksheetId,
                        assignToUserId, u.getUser().getId()));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/submit")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Submit worksheet for review (DRAFT → SUBMITTED)")
    public ResponseEntity<WorksheetMaster> submit(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.submit(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    @PostMapping("/{worksheetId}/start-review")
    @PreAuthorize("hasAuthority('WORKSHEET_REVIEW')")
    @Operation(summary = "Start review of a submitted worksheet (SUBMITTED → UNDER_REVIEW)")
    public ResponseEntity<WorksheetMaster> startReview(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.startReview(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    @PostMapping("/{worksheetId}/approve")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Approve a worksheet (UNDER_REVIEW → APPROVED)")
    public ResponseEntity<WorksheetMaster> approve(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.get("comments") : null;
        return ResponseEntity.ok(
                worksheetService.approve(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), comments));
    }

    @PostMapping("/{worksheetId}/reject")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Reject a worksheet (UNDER_REVIEW → REJECTED)")
    public ResponseEntity<WorksheetMaster> reject(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.get("comments") : null;
        return ResponseEntity.ok(
                worksheetService.reject(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), comments));
    }

    @PostMapping("/{worksheetId}/close")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Close an approved worksheet (APPROVED → CLOSED)")
    public ResponseEntity<WorksheetMaster> close(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.get("comments") : null;
        return ResponseEntity.ok(
                worksheetService.close(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), comments));
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/archive")
    @PreAuthorize("hasAuthority('WORKSHEET_ARCHIVE')")
    @Operation(summary = "Archive a worksheet")
    public ResponseEntity<WorksheetMaster> archive(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.archive(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    @PostMapping("/{worksheetId}/unarchive")
    @PreAuthorize("hasAuthority('WORKSHEET_ARCHIVE')")
    @Operation(summary = "Unarchive a worksheet")
    public ResponseEntity<WorksheetMaster> unarchive(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.unarchive(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    // ── Execution Data ────────────────────────────────────────────────────────

    @GetMapping("/{worksheetId}/execution-data")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get all execution data entries for a worksheet")
    public ResponseEntity<List<WorksheetExecutionData>> getExecutionData(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.getExecutionData(u.getTenantId(), branchId, worksheetId));
    }

    @PostMapping("/{worksheetId}/execution-data")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Add a single execution data entry",
               description = "Body fields: fieldId, fieldName, value, unit, chemicalId, instrumentId, comment, reason.")
    public ResponseEntity<WorksheetExecutionData> addExecutionData(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                worksheetService.saveExecutionData(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), body));
    }

    @PutMapping("/{worksheetId}/execution-data")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Replace all execution data for a worksheet (bulk upsert)",
               description = "Body: array of execution data objects. Replaces ALL existing data for the worksheet.")
    public ResponseEntity<Void> replaceExecutionData(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @RequestBody List<Map<String, Object>> rows,
            @AuthenticationPrincipal LimsUserDetails u) {
        worksheetService.replaceExecutionData(u.getTenantId(), branchId, worksheetId,
                u.getUser().getId(), rows);
        return ResponseEntity.noContent().build();
    }

    // ── Review History ────────────────────────────────────────────────────────

    @GetMapping("/{worksheetId}/review-history")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get full review/status-change history for a worksheet")
    public ResponseEntity<List<WorksheetReviewHistory>> reviewHistory(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.getReviewHistory(u.getTenantId(), branchId, worksheetId));
    }

    // ── Document Template View ────────────────────────────────────────────────

    @GetMapping("/{worksheetId}/template")
    @PreAuthorize("hasAuthority('DOCUMENT_TEMPLATE_VIEW')")
    @Operation(summary = "Get full document template with test cases, blocks, slots and saved values",
               description = "Returns the structured document for readonly (reviewer) or analyst fill mode. " +
                             "Each test case contains ordered blocks (PARAGRAPH/TABLE/IMAGE/FORMULA) " +
                             "and the list of -- field slots with any already-saved values.")
    public ResponseEntity<WorksheetDocumentService.WorksheetTemplateView> getTemplate(
            @PathVariable Long worksheetId,
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetDocumentService.getTemplateView(u.getTenantId(), branchId, worksheetId));
    }

    // ── Field Value Fill (Analyst) ────────────────────────────────────────────

    @PutMapping("/{worksheetId}/fields/{slotId}")
    @PreAuthorize("hasAuthority('WORKSHEET_FIELD_FILL')")
    @Operation(summary = "Save or update a single -- field value (analyst fill mode)",
               description = "Body: { numericValue, unit, qualifier, comment }. " +
                             "unit options: ml/L/g/kg/mg/µg/mEq/IU/%. " +
                             "qualifier options: EXACT/APPROX/TRACE/ND. " +
                             "Upserts — safe to call multiple times for the same slot.")
    public ResponseEntity<WorksheetFieldValue> saveFieldValue(
            @PathVariable Long worksheetId,
            @PathVariable Long slotId,
            @RequestParam Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {

        BigDecimal numericValue = body.containsKey("numericValue") && body.get("numericValue") != null
                ? new BigDecimal(body.get("numericValue").toString()) : null;
        String unit      = (String) body.get("unit");
        String qualifier = (String) body.get("qualifier");
        String comment   = (String) body.get("comment");

        return ResponseEntity.ok(
                worksheetDocumentService.saveFieldValue(
                        u.getTenantId(), branchId, worksheetId, u.getUser().getId(),
                        slotId, numericValue, unit, qualifier, comment));
    }

    // ── Formula Computation ───────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/test-cases/{testCaseId}/compute")
    @PreAuthorize("hasAuthority('WORKSHEET_RESULT_COMPUTE')")
    @Operation(summary = "Compute formula result for a test case after all slots are filled",
               description = "Evaluates the formula expression with analyst-supplied values. " +
                             "Body: { resultUnit } (optional unit for the computed result). " +
                             "Returns the stored WorksheetTestCaseResult.")
    public ResponseEntity<WorksheetTestCaseResult> computeResult(
            @PathVariable Long worksheetId,
            @PathVariable Long testCaseId,
            @RequestParam Long branchId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {

        String resultUnit = body != null ? body.get("resultUnit") : null;
        return ResponseEntity.ok(
                worksheetDocumentService.computeResult(
                        u.getTenantId(), branchId, worksheetId, testCaseId,
                        u.getUser().getId(), resultUnit));
    }

    // ── Result Review ─────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/test-cases/{testCaseId}/review")
    @PreAuthorize("hasAuthority('WORKSHEET_RESULT_REVIEW')")
    @Operation(summary = "Reviewer marks a test case result as PASS or FAIL",
               description = "Body: { passFail: 'PASS'|'FAIL', comments: '...' }.")
    public ResponseEntity<WorksheetTestCaseResult> reviewResult(
            @PathVariable Long worksheetId,
            @PathVariable Long testCaseId,
            @RequestParam Long branchId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {

        String passFail  = body.get("passFail");
        String comments  = body.get("comments");
        return ResponseEntity.ok(
                worksheetDocumentService.reviewResult(
                        u.getTenantId(), branchId, worksheetId, testCaseId,
                        u.getUser().getId(), passFail, comments));
    }
}
