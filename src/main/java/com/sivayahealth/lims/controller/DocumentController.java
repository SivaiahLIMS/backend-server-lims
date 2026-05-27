package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.repository.DocumentVersionRepository;
import com.sivayahealth.lims.repository.WorksheetExecutionRepository;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Map;

@RestController
@Tag(name = "Document Module", description = "DOCX upload, parsing, lifecycle, worksheet execution")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentVersionRepository documentVersionRepository;
    private final WorksheetExecutionRepository worksheetExecutionRepository;

    // ────────────────────────────────────────────
    // Document Master
    // ────────────────────────────────────────────

    @PostMapping("/documents")
    @Operation(summary = "Create a document master entry (before uploading a DOCX)")
    public ResponseEntity<DocumentMaster> createDocument(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.createDocument(
                        u.getTenantId(),
                        (String) body.get("name"),
                        (String) body.get("type"),
                        u.getUser().getId()
                )
        );
    }

    @GetMapping("/documents")
    @Operation(summary = "Get all active documents for the tenant")
    public ResponseEntity<List<DocumentMaster>> getDocuments(@AuthenticationPrincipal LimsUserDetails u) {
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
    @Operation(summary = "Upload a DOCX file and auto-parse it into a JSON schema")
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
    @Operation(summary = "List all versions for a document")
    public ResponseEntity<List<DocumentVersion>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getVersions(id));
    }

    @GetMapping("/documents/{id}/versions/{v}/parsed")
    @Operation(summary = "Get the parsed JSON schema for a specific version")
    public ResponseEntity<DocumentParsedJson> getParsed(@PathVariable Long id, @PathVariable int v) {
        return ResponseEntity.ok(documentService.getParsedJson(id, v));
    }

    // ────────────────────────────────────────────
    // Lifecycle transitions
    // ────────────────────────────────────────────

    @PostMapping("/documents/{id}/versions/{v}/submit-review")
    @Operation(summary = "Submit version for review (DRAFT → UNDER_REVIEW)")
    public ResponseEntity<DocumentVersion> submitForReview(
            @PathVariable Long id, @PathVariable int v,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.submitForReview(id, v, u.getUser().getId()));
    }

    @PostMapping("/documents/{id}/versions/{v}/approve")
    @Operation(summary = "Approve version (UNDER_REVIEW → APPROVED)")
    public ResponseEntity<DocumentVersion> approve(
            @PathVariable Long id, @PathVariable int v,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(documentService.approveVersion(id, v, u.getUser().getId(), comment));
    }

    @PostMapping("/documents/{id}/versions/{v}/publish")
    @Operation(summary = "Publish version (APPROVED → PUBLISHED)")
    public ResponseEntity<DocumentVersion> publish(
            @PathVariable Long id, @PathVariable int v,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.publishVersion(id, v, u.getUser().getId()));
    }

    @PostMapping("/documents/{id}/versions/{v}/retire")
    @Operation(summary = "Retire version (PUBLISHED → RETIRED)")
    public ResponseEntity<DocumentVersion> retire(
            @PathVariable Long id, @PathVariable int v,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.retireVersion(id, v, u.getUser().getId()));
    }

    // ────────────────────────────────────────────
    // Worksheet Execution
    // ────────────────────────────────────────────

    @PostMapping("/worksheets/{documentId}/submit")
    @Operation(summary = "Submit a filled worksheet")
    public ResponseEntity<WorksheetExecution> submitWorksheet(
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                documentService.submitWorksheet(
                        documentId,
                        body.containsKey("sampleId") ? ((Number) body.get("sampleId")).longValue() : null,
                        (String) body.get("filledJson"),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/worksheets/{executionId}/approve")
    @Operation(summary = "Approve a worksheet execution")
    public ResponseEntity<WorksheetExecution> approveWorksheet(
            @PathVariable Long executionId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.approveWorksheet(executionId, u.getUser().getId()));
    }

    @PostMapping("/worksheets/{executionId}/reject")
    @Operation(summary = "Reject a worksheet execution")
    public ResponseEntity<WorksheetExecution> rejectWorksheet(
            @PathVariable Long executionId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentService.rejectWorksheet(executionId, u.getUser().getId()));
    }

    // ────────────────────────────────────────────
    // Document Review Operational Lists
    // ────────────────────────────────────────────

    @GetMapping("/documents/lists/under-review")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "All document versions currently UNDER_REVIEW",
            description = "Includes both assigned and unassigned versions.")
    public ResponseEntity<List<DocumentVersion>> getUnderReview(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentVersionRepository.findByTenantAndState(u.getTenantId(), "UNDER_REVIEW"));
    }

    @GetMapping("/documents/lists/assigned-to-me")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "Documents assigned to the current QC reviewer",
            description = "UNDER_REVIEW versions where reviewedBy = current user.")
    public ResponseEntity<List<DocumentVersion>> getAssignedToMe(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                documentVersionRepository.findAssignedToReviewer(u.getTenantId(), u.getUser().getId()));
    }

    @GetMapping("/documents/lists/unassigned-review-queue")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "UNDER_REVIEW documents not yet assigned to any QC reviewer")
    public ResponseEntity<List<DocumentVersion>> getUnassignedReviewQueue(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentVersionRepository.findUnassignedUnderReview(u.getTenantId()));
    }

    @GetMapping("/documents/lists/approved-for-testing")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "PUBLISHED templates approved by QC — ready for worksheet execution")
    public ResponseEntity<List<DocumentVersion>> getApprovedForTesting(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentVersionRepository.findPublishedForTenant(u.getTenantId()));
    }

    // ────────────────────────────────────────────
    // Test Result / Worksheet Review Lists
    // ────────────────────────────────────────────

    @GetMapping("/worksheets/lists/pending-approval")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "All worksheets pending approval — SUBMITTED status",
            description = "QC/QA review queue: all SUBMITTED worksheet executions across the tenant.")
    public ResponseEntity<List<WorksheetExecution>> getPendingApproval(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetExecutionRepository.findPendingApprovalByTenant(u.getTenantId()));
    }

    @GetMapping("/worksheets/lists/rejected")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Rejected worksheets requiring rework by analyst",
            description = "All REJECTED worksheet executions — analysts need to correct and re-submit.")
    public ResponseEntity<List<WorksheetExecution>> getRejected(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetExecutionRepository.findRejectedByTenant(u.getTenantId()));
    }

    @GetMapping("/worksheets/lists/my-pending")
    @PreAuthorize("hasAuthority('TEST_EXECUTE')")
    @Operation(summary = "My submitted worksheets awaiting approval")
    public ResponseEntity<List<WorksheetExecution>> getMyPending(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                worksheetExecutionRepository.findMyPending(u.getTenantId(), u.getUser().getId()));
    }

    @GetMapping("/worksheets/lists/all")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "All worksheet executions for the tenant (full test result review list)",
            description = "Returns all executions regardless of status, ordered by most recent first.")
    public ResponseEntity<List<WorksheetExecution>> getAllWorksheets(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(worksheetExecutionRepository.findAllByTenant(u.getTenantId()));
    }
}
