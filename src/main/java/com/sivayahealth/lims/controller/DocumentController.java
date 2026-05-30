package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.document.ApproveDocumentRequest;
import com.sivayahealth.lims.dto.document.CreateDocumentRequest;
import com.sivayahealth.lims.dto.document.SubmitWorksheetRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.repository.DocumentVersionRepository;
import com.sivayahealth.lims.repository.WorksheetExecutionRepository;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Document Module", description = "DOCX upload, parsing, lifecycle, and worksheet execution. " +
                                             "Worksheet execution paths are under /document-executions " +
                                             "to avoid collision with /worksheets (WorksheetMaster).")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentVersionRepository documentVersionRepository;
    private final WorksheetExecutionRepository worksheetExecutionRepository;

    // ── Document Master ───────────────────────────────────────────────────────

    @PostMapping("/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE')")
    @Operation(summary = "Create a document master entry",
               description = "Requires: DOCUMENT_CREATE. " +
                             "Creates the master record before uploading a DOCX version.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<DocumentMaster> createDocument(
            @RequestBody CreateDocumentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.createDocument(
                        u.getTenantId(),
                        body.getName(),
                        body.getType(),
                        u.getUser().getId()
                )
        );
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "Get all active documents for the tenant",
               description = "Requires: DOCUMENT_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<DocumentMaster>> getDocuments(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.getDocuments(u.getTenantId()));
    }

    // ────────────────────────────────────────────
    // Document Versions (DOCX upload + POI parse)
    // ────────────────────────────────────────────

    /**
     * Upload a DOCX file.
     * Apache POI parses the document and stores the JSON schema automatically.
     * The version starts at DRAFT and follows:  DRAFT → UNDER_REVIEW → APPROVED → PUBLISHED → RETIRED
     */
    @PostMapping(value = "/documents/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_VERSION_UPLOAD')")
    @Operation(summary = "Upload a DOCX file and auto-parse it into a JSON schema",
               description = "Requires: DOCUMENT_VERSION_UPLOAD. " +
                             "POI parses the DOCX and stores the JSON schema. " +
                             "Lifecycle: DRAFT → UNDER_REVIEW → APPROVED → PUBLISHED → RETIRED. " +
                             "Requires X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Uploaded and parsed"),
        @ApiResponse(responseCode = "400", description = "Invalid file or missing headers"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<DocumentVersion> uploadDocx(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.uploadDocxVersion(id, u.getTenantId(), branchId, file, u.getUser().getId())
        );
    }

    @GetMapping("/documents/{id}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "List all versions for a document",
               description = "Requires: DOCUMENT_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<List<DocumentVersion>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getVersions(id));
    }

    @GetMapping("/documents/{id}/versions/{v}/parsed")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "Get the parsed JSON schema for a specific version",
               description = "Requires: DOCUMENT_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public ResponseEntity<DocumentParsedJson> getParsed(
            @PathVariable Long id, @PathVariable int v) {
        return ResponseEntity.ok(documentService.getParsedJson(id, v));
    }

    // ────────────────────────────────────────────
    // Lifecycle transitions
    // ────────────────────────────────────────────

    @PostMapping("/documents/{id}/versions/{v}/submit-review")
    @PreAuthorize("hasAuthority('DOCUMENT_SUBMIT')")
    @Operation(summary = "Submit version for review (DRAFT → UNDER_REVIEW)",
               description = "Requires: DOCUMENT_SUBMIT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submitted"),
        @ApiResponse(responseCode = "400", description = "Version not in DRAFT state"),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public ResponseEntity<DocumentVersion> submitForReview(
            @PathVariable Long id, @PathVariable int v,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.submitForReview(id, v, u.getUser().getId()));
    }

    @PostMapping("/documents/{id}/versions/{v}/approve")
    @PreAuthorize("hasAuthority('DOCUMENT_APPROVE')")
    @Operation(summary = "Approve version (UNDER_REVIEW → APPROVED)",
               description = "Requires: DOCUMENT_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "400", description = "Version not in UNDER_REVIEW state"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<DocumentVersion> approve(
            @PathVariable Long id, @PathVariable int v,
            @RequestBody(required = false) ApproveDocumentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.getComment() : null;
        return ResponseEntity.ok(documentService.approveVersion(id, v, u.getUser().getId(), comment));
    }

    @PostMapping("/documents/{id}/versions/{v}/publish")
    @PreAuthorize("hasAuthority('DOCUMENT_PUBLISH')")
    @Operation(summary = "Publish version (APPROVED → PUBLISHED)",
               description = "Requires: DOCUMENT_PUBLISH")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Published"),
        @ApiResponse(responseCode = "400", description = "Version not in APPROVED state")
    })
    public ResponseEntity<DocumentVersion> publish(
            @PathVariable Long id, @PathVariable int v,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.publishVersion(id, v, u.getUser().getId()));
    }

    @PostMapping("/documents/{id}/versions/{v}/retire")
    @PreAuthorize("hasAuthority('DOCUMENT_RETIRE')")
    @Operation(summary = "Retire version (PUBLISHED → RETIRED)",
               description = "Requires: DOCUMENT_RETIRE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Retired"),
        @ApiResponse(responseCode = "400", description = "Version not in PUBLISHED state")
    })
    public ResponseEntity<DocumentVersion> retire(
            @PathVariable Long id, @PathVariable int v,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.retireVersion(id, v, u.getUser().getId()));
    }

    // ── Document Review Queues ────────────────────────────────────────────────

    @GetMapping("/documents/lists/under-review")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "All document versions currently UNDER_REVIEW",
               description = "Requires: DOCUMENT_VIEW. Includes assigned and unassigned versions.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<DocumentVersion>> getUnderReview(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                documentVersionRepository.findByTenantAndState(u.getTenantId(), "UNDER_REVIEW"));
    }

    @GetMapping("/documents/lists/assigned-to-me")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "Documents assigned to the current QC reviewer",
               description = "Requires: DOCUMENT_VIEW. UNDER_REVIEW versions where reviewedBy = current user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<DocumentVersion>> getAssignedToMe(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                documentVersionRepository.findAssignedToReviewer(
                        u.getTenantId(), u.getUser().getId()));
    }

    @GetMapping("/documents/lists/unassigned-review-queue")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "UNDER_REVIEW documents not yet assigned to any QC reviewer",
               description = "Requires: DOCUMENT_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<DocumentVersion>> getUnassignedReviewQueue(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                documentVersionRepository.findUnassignedUnderReview(u.getTenantId()));
    }

    @GetMapping("/documents/lists/approved-for-testing")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "PUBLISHED templates approved by QC — ready for worksheet execution",
               description = "Requires: DOCUMENT_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<DocumentVersion>> getApprovedForTesting(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                documentVersionRepository.findPublishedForTenant(u.getTenantId()));
    }

    // ── Worksheet Execution (renamed from /worksheets to /document-executions) ──
    //
    // These endpoints handle the legacy WorksheetExecution entity (document-based fill-and-submit).
    // They are intentionally separate from /worksheets (WorksheetMaster lifecycle).

    @PostMapping("/document-executions")
    @PreAuthorize("hasAuthority('TEST_EXECUTE')")
    @Operation(summary = "Submit a filled worksheet execution",
               description = "Requires: TEST_EXECUTE. " +
                             "Creates a WorksheetExecution record for a document. " +
                             "NOTE: This is the legacy document-fill flow. " +
                             "For the structured test-case flow, use /worksheets/{id}/template.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Submitted"),
        @ApiResponse(responseCode = "400", description = "Missing documentId or filledJson"),
        @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<WorksheetExecution> submitWorksheet(
            @RequestParam Long documentId,
            @RequestBody SubmitWorksheetRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.submitWorksheet(
                        documentId,
                        body.getSampleId(),
                        body.getFilledJson(),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/document-executions/{executionId}/approve")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Approve a worksheet execution",
               description = "Requires: RESULT_REVIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Execution not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<WorksheetExecution> approveWorksheet(
            @PathVariable Long executionId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.approveWorksheet(executionId, u.getUser().getId()));
    }

    @PostMapping("/document-executions/{executionId}/reject")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Reject a worksheet execution",
               description = "Requires: RESULT_REVIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rejected"),
        @ApiResponse(responseCode = "404", description = "Execution not found")
    })
    public ResponseEntity<WorksheetExecution> rejectWorksheet(
            @PathVariable Long executionId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.rejectWorksheet(executionId, u.getUser().getId()));
    }

    // ── Execution Review Queues ───────────────────────────────────────────────

    @GetMapping("/document-executions/lists/pending-approval")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "All worksheet executions pending approval (SUBMITTED)",
               description = "Requires: RESULT_REVIEW. QC/QA review queue across the tenant.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<WorksheetExecution>> getPendingApproval(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetExecutionRepository.findPendingApprovalByTenant(u.getTenantId()));
    }

    @GetMapping("/document-executions/lists/rejected")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Rejected worksheet executions requiring rework",
               description = "Requires: RESULT_REVIEW. Analysts need to correct and re-submit.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<WorksheetExecution>> getRejected(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetExecutionRepository.findRejectedByTenant(u.getTenantId()));
    }

    @GetMapping("/document-executions/lists/my-pending")
    @PreAuthorize("hasAuthority('TEST_EXECUTE')")
    @Operation(summary = "Current user's submitted executions awaiting approval",
               description = "Requires: TEST_EXECUTE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<WorksheetExecution>> getMyPending(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetExecutionRepository.findMyPending(
                        u.getTenantId(), u.getUser().getId()));
    }

    @GetMapping("/document-executions/lists/all")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "All worksheet executions for the tenant",
               description = "Requires: RESULT_REVIEW. Returns all executions regardless of status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<WorksheetExecution>> getAllWorksheets(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetExecutionRepository.findAllByTenant(u.getTenantId()));
    }
}
