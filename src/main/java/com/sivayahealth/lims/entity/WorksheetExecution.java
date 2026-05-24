package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_execution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetExecution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentMaster document;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "sample_id")
    private Long sampleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by")
    private AppUser executedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "filled_json", nullable = false, columnDefinition = "JSONB")
    private String filledJson;
}
