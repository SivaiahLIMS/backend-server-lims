package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_version")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentMaster document;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "lifecycle_state", nullable = false)
    private String lifecycleState = "DRAFT";

    // ── File storage ──────────────────────────────────────
    @Column(name = "original_filename")
    private String originalFilename;

    /** Supabase Storage path: {tenantId}/{documentId}/v{versionNo}/{filename} */
    @Column(name = "storage_path")
    private String storagePath;

    /** Long-lived signed URL returned by Supabase Storage */
    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    // ── Upload audit ──────────────────────────────────────
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private AppUser uploadedBy;

    // ── Review audit ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    // ── Approval audit ────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AppUser approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ── Publish audit ─────────────────────────────────────
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private AppUser publishedBy;

    // ── Retire audit ──────────────────────────────────────
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retired_by")
    private AppUser retiredBy;
}
