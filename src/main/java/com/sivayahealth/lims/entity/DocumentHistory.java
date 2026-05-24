package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentMaster document;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private AppUser archivedBy;
}
