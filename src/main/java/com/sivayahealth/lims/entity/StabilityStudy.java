package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stability_study")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StabilityStudy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "study_code", unique = true, length = 50)
    private String studyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductMaster product;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "study_type", nullable = false, length = 30)
    private String studyType;

    @Column(columnDefinition = "TEXT")
    private String protocol;

    @Column(name = "storage_condition", length = 200)
    private String storageCondition;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "study", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StabilityStudyTimepoint> timepoints = new ArrayList<>();
}
