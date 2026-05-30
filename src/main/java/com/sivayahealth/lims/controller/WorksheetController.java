package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.worksheet.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.WorksheetDocumentService;
import com.sivayahealth.lims.service.WorksheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worksheets")
@RequiredArgsConstructor
@Tag(name = "Worksheet Management",
     description = "Worksheet lifecycle, execution data, and review history. " +
                   "Requires X-Branch-Id header on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class WorksheetController {

    private final WorksheetService         worksheetService;
    private final WorksheetDocumentService  worksheetDocumentService;

    // ── List / Search ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List all worksheets", description = "Requires: WORKSHEET_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<WorksheetMaster>> listAll(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listAll(u.getTenantId(), branchId));
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List worksheets by status",
               description = "Requires: WORKSHEET_VIEW. " +
                             "Valid statuses: DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, CLOSED")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<WorksheetMaster>> listByStatus(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listByStatus(u.getTenantId(), branchId, status));
    }

    @GetMapping("/archived")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List archived worksheets", description = "Requires: WORKSHEET_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<WorksheetMaster>> listArchived(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listArchived(u.getTenantId(), branchId));
    }

    @GetMapping("/assigned-to/{userId}")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "List worksheets assigned to a specific user",
               description = "Requires: WORKSHEET_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<WorksheetMaster>> listAssignedTo(
            @PathVariable Long userId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.listAssignedTo(u.getTenantId(), branchId, userId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Search worksheets with optional filters",
               description = "Requires: WORKSHEET_VIEW. " +
                             "All filters optional: status, isArchived, productId, assignedToId, batchNo, from, to")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid filter values")
    })
    public ResponseEntity<List<WorksheetMaster>> search(
            @RequestHeader("X-Branch-Id") Long branchId,
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
    @Operation(summary = "Get a single worksheet by ID", description = "Requires: WORKSHEET_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<WorksheetMaster> getById(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetService.getById(u.getTenantId(), branchId, worksheetId));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('WORKSHEET_CREATE')")
    @Operation(summary = "Create a new worksheet (status: DRAFT)",
               description = "Requires: WORKSHEET_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<WorksheetMaster> create(
            @RequestBody CreateWorksheetRequest body,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        WorksheetMaster data = new WorksheetMaster();
        data.setBatchNo(body.getBatchNo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(worksheetService.create(u.getTenantId(), branchId, u.getUser().getId(), data));
    }

    @PutMapping("/{worksheetId}")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Update worksheet fields (only DRAFT or REJECTED)",
               description = "Requires: WORKSHEET_EDIT. Editable fields: batchNo, productId, templateId, documentVersionId.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in editable state"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<WorksheetMaster> update(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody UpdateWorksheetRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        Map<String, Object> fields = Map.of(
            "batchNo",           body.getBatchNo() != null ? body.getBatchNo() : "",
            "productId",         body.getProductId(),
            "templateId",        body.getTemplateId(),
            "documentVersionId", body.getDocumentVersionId()
        );
        return ResponseEntity.ok(
                worksheetService.update(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), fields));
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/assign")
    @PreAuthorize("hasAuthority('WORKSHEET_ASSIGN')")
    @Operation(summary = "Assign a worksheet to an analyst",
               description = "Requires: WORKSHEET_ASSIGN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assigned"),
        @ApiResponse(responseCode = "404", description = "Worksheet or user not found")
    })
    public ResponseEntity<WorksheetMaster> assign(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody AssignWorksheetRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.assign(u.getTenantId(), branchId, worksheetId,
                        body.getAssignToUserId(), u.getUser().getId()));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/submit")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Submit worksheet for review (DRAFT → SUBMITTED)",
               description = "Requires: WORKSHEET_EDIT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submitted"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in DRAFT state"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<WorksheetMaster> submit(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.submit(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    @PostMapping("/{worksheetId}/start-review")
    @PreAuthorize("hasAuthority('WORKSHEET_REVIEW')")
    @Operation(summary = "Start review (SUBMITTED → UNDER_REVIEW)",
               description = "Requires: WORKSHEET_REVIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Under review"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in SUBMITTED state")
    })
    public ResponseEntity<WorksheetMaster> startReview(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.startReview(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    @PostMapping("/{worksheetId}/approve")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Approve worksheet (UNDER_REVIEW → APPROVED)",
               description = "Requires: WORKSHEET_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in UNDER_REVIEW state"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<WorksheetMaster> approve(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) ReviewWorksheetRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.getComments() : null;
        return ResponseEntity.ok(
                worksheetService.approve(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), comments));
    }

    @PostMapping("/{worksheetId}/reject")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Reject worksheet (UNDER_REVIEW → REJECTED)",
               description = "Requires: WORKSHEET_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rejected"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in UNDER_REVIEW state")
    })
    public ResponseEntity<WorksheetMaster> reject(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) ReviewWorksheetRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.getComments() : null;
        return ResponseEntity.ok(
                worksheetService.reject(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), comments));
    }

    @PostMapping("/{worksheetId}/close")
    @PreAuthorize("hasAuthority('WORKSHEET_APPROVE')")
    @Operation(summary = "Close approved worksheet (APPROVED → CLOSED)",
               description = "Requires: WORKSHEET_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Closed"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in APPROVED state")
    })
    public ResponseEntity<WorksheetMaster> close(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) ReviewWorksheetRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.getComments() : null;
        return ResponseEntity.ok(
                worksheetService.close(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), comments));
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/archive")
    @PreAuthorize("hasAuthority('WORKSHEET_ARCHIVE')")
    @Operation(summary = "Archive a worksheet", description = "Requires: WORKSHEET_ARCHIVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archived"),
        @ApiResponse(responseCode = "409", description = "Already archived")
    })
    public ResponseEntity<WorksheetMaster> archive(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.archive(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    @PostMapping("/{worksheetId}/unarchive")
    @PreAuthorize("hasAuthority('WORKSHEET_ARCHIVE')")
    @Operation(summary = "Unarchive a worksheet", description = "Requires: WORKSHEET_ARCHIVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unarchived"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<WorksheetMaster> unarchive(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.unarchive(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId()));
    }

    // ── Execution Data ────────────────────────────────────────────────────────

    @GetMapping("/{worksheetId}/execution-data")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get all execution data entries for a worksheet",
               description = "Requires: WORKSHEET_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<List<WorksheetExecutionData>> getExecutionData(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.getExecutionData(u.getTenantId(), branchId, worksheetId));
    }

    @PostMapping("/{worksheetId}/execution-data")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Add a single execution data entry",
               description = "Requires: WORKSHEET_EDIT. " +
                             "Fields: fieldId, fieldName, value, unit, chemicalId, instrumentId, comment, reason.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid data"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<WorksheetExecutionData> addExecutionData(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                worksheetService.saveExecutionData(u.getTenantId(), branchId, worksheetId,
                        u.getUser().getId(), body));
    }

    @PutMapping("/{worksheetId}/execution-data")
    @PreAuthorize("hasAuthority('WORKSHEET_EDIT')")
    @Operation(summary = "Replace all execution data (bulk upsert)",
               description = "Requires: WORKSHEET_EDIT. Replaces ALL existing data for the worksheet.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Replaced"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<Void> replaceExecutionData(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody List<Map<String, Object>> rows,
            @AuthenticationPrincipal LimsUserDetails u) {
        worksheetService.replaceExecutionData(u.getTenantId(), branchId, worksheetId,
                u.getUser().getId(), rows);
        return ResponseEntity.noContent().build();
    }

    // ── Review History ────────────────────────────────────────────────────────

    @GetMapping("/{worksheetId}/review-history")
    @PreAuthorize("hasAuthority('WORKSHEET_VIEW')")
    @Operation(summary = "Get full review/status-change history for a worksheet",
               description = "Requires: WORKSHEET_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<List<WorksheetReviewHistory>> reviewHistory(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetService.getReviewHistory(u.getTenantId(), branchId, worksheetId));
    }

    // ── Document Template View ────────────────────────────────────────────────

    @GetMapping("/{worksheetId}/template")
    @PreAuthorize("hasAuthority('DOCUMENT_TEMPLATE_VIEW')")
    @Operation(summary = "Get structured document template with test cases, blocks, and field slots",
               description = "Requires: DOCUMENT_TEMPLATE_VIEW. " +
                             "Returns ordered test cases (each ending with a FORMULA block), " +
                             "field slots, and any saved analyst values.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "No document version linked"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found")
    })
    public ResponseEntity<WorksheetDocumentService.WorksheetTemplateView> getTemplate(
            @PathVariable Long worksheetId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetDocumentService.getTemplateView(u.getTenantId(), branchId, worksheetId));
    }

    // ── Field Value Fill (Analyst) ────────────────────────────────────────────

    @PutMapping("/{worksheetId}/fields/{slotId}")
    @PreAuthorize("hasAuthority('WORKSHEET_FIELD_FILL')")
    @Operation(summary = "Save or update a -- field value (analyst fill mode)",
               description = "Requires: WORKSHEET_FIELD_FILL. " +
                             "unit: ml/L/g/kg/mg/µg/mEq/IU/%. qualifier: EXACT/APPROX/TRACE/ND. " +
                             "Safe to call multiple times — upserts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saved"),
        @ApiResponse(responseCode = "400", description = "Worksheet not in fillable state"),
        @ApiResponse(responseCode = "404", description = "Slot not found")
    })
    public ResponseEntity<WorksheetFieldValue> saveFieldValue(
            @PathVariable Long worksheetId,
            @PathVariable Long slotId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody SaveFieldValueRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetDocumentService.saveFieldValue(
                        u.getTenantId(), branchId, worksheetId, u.getUser().getId(),
                        slotId, body.getNumericValue(), body.getUnit(),
                        body.getQualifier(), body.getComment()));
    }

    // ── Formula Computation ───────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/test-cases/{testCaseId}/compute")
    @PreAuthorize("hasAuthority('WORKSHEET_RESULT_COMPUTE')")
    @Operation(summary = "Compute formula result for a test case",
               description = "Requires: WORKSHEET_RESULT_COMPUTE. " +
                             "All field slots must be filled before calling. " +
                             "Evaluates formula_expression with analyst values.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Result computed"),
        @ApiResponse(responseCode = "400", description = "Missing slot values or formula evaluation error"),
        @ApiResponse(responseCode = "404", description = "Test case not found")
    })
    public ResponseEntity<WorksheetTestCaseResult> computeResult(
            @PathVariable Long worksheetId,
            @PathVariable Long testCaseId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) ComputeResultRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String resultUnit = body != null ? body.getResultUnit() : null;
        return ResponseEntity.ok(
                worksheetDocumentService.computeResult(
                        u.getTenantId(), branchId, worksheetId, testCaseId,
                        u.getUser().getId(), resultUnit));
    }

    // ── Result Review ─────────────────────────────────────────────────────────

    @PostMapping("/{worksheetId}/test-cases/{testCaseId}/review")
    @PreAuthorize("hasAuthority('WORKSHEET_RESULT_REVIEW')")
    @Operation(summary = "Reviewer marks a test case result as PASS or FAIL",
               description = "Requires: WORKSHEET_RESULT_REVIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviewed"),
        @ApiResponse(responseCode = "400", description = "Invalid passFail value or no computed result"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Result not found")
    })
    public ResponseEntity<WorksheetTestCaseResult> reviewResult(
            @PathVariable Long worksheetId,
            @PathVariable Long testCaseId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ReviewResultRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetDocumentService.reviewResult(
                        u.getTenantId(), branchId, worksheetId, testCaseId,
                        u.getUser().getId(), body.getPassFail(), body.getComments()));
    }
}
