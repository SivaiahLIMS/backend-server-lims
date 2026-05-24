package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_test_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_execution_id", nullable = false)
    private WorksheetExecution worksheetExecution;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "value_numeric")
    private BigDecimal valueNumeric;

    @Column(name = "value_text")
    private String valueText;

    private String unit;

    @Column(name = "lower_limit")
    private BigDecimal lowerLimit;

    @Column(name = "upper_limit")
    private BigDecimal upperLimit;

    @Column(name = "is_oos", nullable = false)
    private boolean oos = false;

    @Column(name = "is_oot", nullable = false)
    private boolean oot = false;

    @Column(name = "oos_reason", columnDefinition = "TEXT")
    private String oosReason;

    @Column(name = "oos_detected_at")
    private LocalDateTime oosDetectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oos_detected_by")
    private AppUser oosDetectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oos_investigation_task_id")
    private TaskMaster oosInvestigationTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
