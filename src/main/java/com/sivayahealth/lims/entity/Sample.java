package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sample")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sample {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "sample_no", nullable = false, length = 100)
    private String sampleNo;

    @Column(name = "sample_code", length = 100)
    private String sampleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_type_id")
    private SampleType sampleTypeRef;

    @Column(name = "sample_type", length = 100)
    private String sampleType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_batch_id")
    private SampleBatch sampleBatch;

    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SampleStatus status = SampleStatus.REGISTERED;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "storage_location_id")
    private Long storageLocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
