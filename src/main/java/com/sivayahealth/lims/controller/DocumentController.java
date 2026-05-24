package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
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
}
