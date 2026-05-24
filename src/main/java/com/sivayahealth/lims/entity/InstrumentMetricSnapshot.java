package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_metric_snapshot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private InstrumentMaster instrument;

    @Column(name = "metric_type")
    private String metricType;

    @Column(name = "metric_value")
    private BigDecimal metricValue;

    @Column(name = "metric_date")
    private LocalDate metricDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
