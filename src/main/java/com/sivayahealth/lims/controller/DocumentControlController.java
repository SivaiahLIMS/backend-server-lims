package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.doccontrol.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.DocumentControlService;
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

@RestController
@RequestMapping("/documents/control")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Document Control", description = "Document lifecycle: DRAFT → UNDER_REVIEW → APPROVED → PUBLISHED → OBSOLETE")
public class DocumentControlController {

    private final DocumentControlService documentControlService;

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "List all controlled documents for tenant")
    public ResponseEntity<List<DocumentControl>> getDocuments(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        if (status != null) {
            return ResponseEntity.ok(documentControlService.getDocumentsByStatus(u.getTenantId(), status));
        }
        return ResponseEntity.ok(documentControlService.getDocuments(u.getTenantId()));
    }

    @GetMapping("/due-for-review")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "List documents due for review within the next 30 days")
    public ResponseEntity<List<DocumentControl>> getDocumentsDueForReview(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.getDocumentsDueForReview(u.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE')")
    @Operation(summary = "Create a new controlled document")
    public ResponseEntity<DocumentControl> createDocument(
            @RequestBody CreateDocControlRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentControlService.createDocument(
                        u.getTenantId(), body.getTitle(), body.getDocType(),
                        body.getCategory(), body.getReviewPeriodMonths(), u.getUser().getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "Get a controlled document by ID")
    public ResponseEntity<DocumentControl> getDocumentById(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.getDocumentById(id));
    }

    @PostMapping("/{id}/obsolete")
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    @Operation(summary = "Mark a controlled document as obsolete")
    public ResponseEntity<DocumentControl> obsoleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.obsoleteDocument(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    @Operation(summary = "Renew a published document — creates a new DRAFT version")
    public ResponseEntity<DocumentControl> renewDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.renewDocument(id, u.getUser().getId()));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "List versions of a controlled document")
    public ResponseEntity<List<DocumentControlVersion>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.getVersions(id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE')")
    @Operation(summary = "Create a new version of a controlled document")
    public ResponseEntity<DocumentControlVersion> createVersion(
            @PathVariable Long id,
            @RequestBody CreateVersionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentControlService.createVersion(id, body.getContent(), body.getChangeSummary(), u.getUser().getId()));
    }

    @PostMapping("/versions/{versionId}/submit")
    @PreAuthorize("hasAuthority('DOCUMENT_EDIT')")
    @Operation(summary = "Submit a version for review")
    public ResponseEntity<DocumentControlVersion> submitForReview(
            @PathVariable Long versionId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.submitVersionForReview(versionId, u.getUser().getId()));
    }

    @PostMapping("/versions/{versionId}/review")
    @PreAuthorize("hasAuthority('DOCUMENT_REVIEW')")
    @Operation(summary = "Mark a version as reviewed")
    public ResponseEntity<DocumentControlVersion> reviewVersion(
            @PathVariable Long versionId,
            @RequestBody(required = false) ReviewVersionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.getReviewComment() : null;
        return ResponseEntity.ok(documentControlService.reviewVersion(versionId, comment, u.getUser().getId()));
    }

    @PostMapping("/versions/{versionId}/approve")
    @PreAuthorize("hasAuthority('DOCUMENT_APPROVE')")
    @Operation(summary = "Approve a document version")
    public ResponseEntity<DocumentControlVersion> approveVersion(
            @PathVariable Long versionId,
            @RequestBody(required = false) ApproveVersionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.getApprovalComment() : null;
        return ResponseEntity.ok(documentControlService.approveVersion(versionId, comment, u.getUser().getId()));
    }

    @PostMapping("/versions/{versionId}/reject")
    @PreAuthorize("hasAuthority('DOCUMENT_APPROVE')")
    @Operation(summary = "Reject a document version")
    public ResponseEntity<DocumentControlVersion> rejectVersion(
            @PathVariable Long versionId,
            @RequestBody RejectVersionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.rejectVersion(versionId, body.getRejectionReason(), u.getUser().getId()));
    }

    @PostMapping("/versions/{versionId}/publish")
    @PreAuthorize("hasAuthority('DOCUMENT_PUBLISH')")
    @Operation(summary = "Publish an approved document version")
    public ResponseEntity<DocumentControlVersion> publishVersion(
            @PathVariable Long versionId,
            @RequestBody(required = false) PublishVersionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        java.time.LocalDate effectiveDate = body != null ? body.getEffectiveDate() : null;
        return ResponseEntity.ok(documentControlService.publishVersion(versionId, effectiveDate, u.getUser().getId()));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    @Operation(summary = "Get audit trail for a controlled document")
    public ResponseEntity<List<DocumentControlAuditTrail>> getAuditTrail(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(documentControlService.getAuditTrail(id));
    }
}
