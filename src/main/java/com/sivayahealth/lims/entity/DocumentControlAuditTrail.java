package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_control_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentControlAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentControl document;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "old_value_json", columnDefinition = "TEXT")
    private String oldValueJson;

    @Column(name = "new_value_json", columnDefinition = "TEXT")
    private String newValueJson;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
