package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "sample_type", length = 100)
    private String sampleType;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
