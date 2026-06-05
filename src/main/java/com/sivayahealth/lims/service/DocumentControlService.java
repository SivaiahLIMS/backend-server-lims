package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentControlService {

    private final DocumentControlRepository documentControlRepository;
    private final DocumentControlVersionRepository versionRepository;
    private final DocumentControlAuditTrailRepository auditTrailRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    @Transactional
    public DocumentControl createDocument(Long tenantId, String title, String docType,
                                          String category, Integer reviewPeriodMonths, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));

        String code = "DOC-" + java.time.Year.now().getValue() + "-" + String.format("%04d", System.currentTimeMillis() % 10000);

        LocalDate reviewDueDate = reviewPeriodMonths != null
                ? LocalDate.now().plusMonths(reviewPeriodMonths)
                : null;

        DocumentControl doc = DocumentControl.builder()
                .tenant(tenant)
                .documentCode(code)
                .title(title)
                .docType(docType)
                .category(category)
                .status("DRAFT")
                .reviewPeriodMonths(reviewPeriodMonths)
                .reviewDueDate(reviewDueDate)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        DocumentControl saved = documentControlRepository.save(doc);

        DocumentControlVersion version = DocumentControlVersion.builder()
                .document(saved)
                .versionNumber(1)
                .status("DRAFT")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        versionRepository.save(version);

        logAudit(saved, "CREATE", null, "DRAFT", userId);
        auditService.log(tenantId, userId, "DocumentControl", saved.getId(), "CREATE", null, "DRAFT");
        return saved;
    }

    @Transactional
    public DocumentControlVersion createVersion(Long documentId, String content, String changeSummary, Long userId) {
        DocumentControl doc = documentControlRepository.findById(documentId)
                .orElseThrow(() -> LimsException.notFound("Document not found"));

        int nextVersion = versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream().findFirst().map(v -> v.getVersionNumber() + 1).orElse(1);

        DocumentControlVersion version = DocumentControlVersion.builder()
                .document(doc)
                .versionNumber(nextVersion)
                .status("DRAFT")
                .content(content)
                .changeSummary(changeSummary)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        DocumentControlVersion saved = versionRepository.save(version);
        logAudit(doc, "NEW_VERSION", null, "v" + nextVersion, userId);
        return saved;
    }

    @Transactional
    public DocumentControlVersion submitVersionForReview(Long versionId, Long userId) {
        DocumentControlVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> LimsException.notFound("Version not found"));
        if (!"DRAFT".equals(version.getStatus())) throw LimsException.badRequest("Version is not in DRAFT status");
        String old = version.getStatus();
        version.setStatus("UNDER_REVIEW");
        DocumentControlVersion saved = versionRepository.save(version);

        DocumentControl doc = version.getDocument();
        doc.setStatus("UNDER_REVIEW");
        documentControlRepository.save(doc);
        logAudit(doc, "SUBMIT_FOR_REVIEW", old, "UNDER_REVIEW", userId);
        return saved;
    }

    @Transactional
    public DocumentControlVersion reviewVersion(Long versionId, String reviewComment, Long userId) {
        DocumentControlVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> LimsException.notFound("Version not found"));
        version.setReviewedBy(userId);
        version.setReviewedAt(LocalDateTime.now());
        version.setReviewComment(reviewComment);
        return versionRepository.save(version);
    }

    @Transactional
    public DocumentControlVersion approveVersion(Long versionId, String approvalComment, Long userId) {
        DocumentControlVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> LimsException.notFound("Version not found"));
        String old = version.getStatus();
        version.setStatus("APPROVED");
        version.setApprovedBy(userId);
        version.setApprovedAt(LocalDateTime.now());
        version.setApprovalComment(approvalComment);
        DocumentControlVersion saved = versionRepository.save(version);

        DocumentControl doc = version.getDocument();
        doc.setStatus("APPROVED");
        documentControlRepository.save(doc);
        logAudit(doc, "APPROVE", old, "APPROVED", userId);
        auditService.log(doc.getTenant().getId(), userId, "DocumentControl", doc.getId(), "APPROVE", old, "APPROVED");
        return saved;
    }

    @Transactional
    public DocumentControlVersion rejectVersion(Long versionId, String rejectionReason, Long userId) {
        DocumentControlVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> LimsException.notFound("Version not found"));
        String old = version.getStatus();
        version.setStatus("REJECTED");
        version.setRejectedBy(userId);
        version.setRejectedAt(LocalDateTime.now());
        version.setRejectionReason(rejectionReason);
        DocumentControlVersion saved = versionRepository.save(version);

        DocumentControl doc = version.getDocument();
        doc.setStatus("REJECTED");
        documentControlRepository.save(doc);
        logAudit(doc, "REJECT", old, "REJECTED", userId);
        auditService.log(doc.getTenant().getId(), userId, "DocumentControl", doc.getId(), "REJECT", old, "REJECTED");
        return saved;
    }

    @Transactional
    public DocumentControlVersion publishVersion(Long versionId, LocalDate effectiveDate, Long userId) {
        DocumentControlVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> LimsException.notFound("Version not found"));
        if (!"APPROVED".equals(version.getStatus())) throw LimsException.badRequest("Version must be APPROVED before publishing");
        String old = version.getStatus();
        version.setStatus("PUBLISHED");
        version.setPublishedBy(userId);
        version.setPublishedAt(LocalDateTime.now());
        version.setEffectiveDate(effectiveDate != null ? effectiveDate : LocalDate.now());
        DocumentControlVersion saved = versionRepository.save(version);

        DocumentControl doc = version.getDocument();
        doc.setStatus("PUBLISHED");

        if (doc.getReviewPeriodMonths() != null) {
            LocalDate base = effectiveDate != null ? effectiveDate : LocalDate.now();
            doc.setReviewDueDate(base.plusMonths(doc.getReviewPeriodMonths()));
            version.setReviewDueDate(doc.getReviewDueDate());
            versionRepository.save(version);
        }

        documentControlRepository.save(doc);
        logAudit(doc, "PUBLISH", old, "PUBLISHED", userId);
        auditService.log(doc.getTenant().getId(), userId, "DocumentControl", doc.getId(), "PUBLISH", old, "PUBLISHED");
        return saved;
    }

    @Transactional
    public DocumentControl renewDocument(Long documentId, Long userId) {
        DocumentControl doc = documentControlRepository.findById(documentId)
                .orElseThrow(() -> LimsException.notFound("Document not found"));

        int nextVersion = versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream().findFirst().map(v -> v.getVersionNumber() + 1).orElse(1);

        DocumentControlVersion newVersion = DocumentControlVersion.builder()
                .document(doc)
                .versionNumber(nextVersion)
                .status("DRAFT")
                .changeSummary("Periodic renewal")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        versionRepository.save(newVersion);

        doc.setStatus("DRAFT");
        if (doc.getReviewPeriodMonths() != null) {
            doc.setReviewDueDate(LocalDate.now().plusMonths(doc.getReviewPeriodMonths()));
        }
        DocumentControl saved = documentControlRepository.save(doc);
        logAudit(saved, "RENEW", "PUBLISHED", "DRAFT", userId);
        auditService.log(doc.getTenant().getId(), userId, "DocumentControl", doc.getId(), "RENEW", "PUBLISHED", "DRAFT v" + nextVersion);
        return saved;
    }

    @Transactional
    public DocumentControl obsoleteDocument(Long documentId, Long userId) {
        DocumentControl doc = documentControlRepository.findById(documentId)
                .orElseThrow(() -> LimsException.notFound("Document not found"));
        String old = doc.getStatus();
        doc.setStatus("OBSOLETE");
        DocumentControl saved = documentControlRepository.save(doc);
        logAudit(saved, "OBSOLETE", old, "OBSOLETE", userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DocumentControl> getDocuments(Long tenantId) {
        return documentControlRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<DocumentControl> getDocumentsByStatus(Long tenantId, String status) {
        return documentControlRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public List<DocumentControl> getDocumentsDueForReview(Long tenantId) {
        return documentControlRepository.findDueForReview(tenantId, LocalDate.now().plusDays(30));
    }

    @Transactional(readOnly = true)
    public DocumentControl getDocumentById(Long id) {
        return documentControlRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Document not found"));
    }

    @Transactional(readOnly = true)
    public List<DocumentControlVersion> getVersions(Long documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
    }

    @Transactional(readOnly = true)
    public List<DocumentControlAuditTrail> getAuditTrail(Long documentId) {
        return auditTrailRepository.findByDocumentIdOrderByPerformedAtAsc(documentId);
    }

    private void logAudit(DocumentControl doc, String action, String oldVal, String newVal, Long userId) {
        DocumentControlAuditTrail entry = DocumentControlAuditTrail.builder()
                .document(doc)
                .action(action)
                .oldValueJson(oldVal)
                .newValueJson(newVal)
                .performedBy(userId)
                .performedAt(LocalDateTime.now())
                .build();
        auditTrailRepository.save(entry);
    }
}
