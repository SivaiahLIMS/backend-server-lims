package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_reading")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private InstrumentMaster instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_execution_id")
    private WorksheetExecution worksheetExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_task_id")
    private CalibrationTask calibrationTask;

    @Column(nullable = false)
    private String mode = "MANUAL";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reading_json", columnDefinition = "jsonb", nullable = false)
    private String readingJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
