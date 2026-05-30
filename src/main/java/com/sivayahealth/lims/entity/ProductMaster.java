package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_master",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id","product_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMaster {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(length = 100)
    private String strength;

    @Column(name = "dosage_form", length = 100)
    private String dosageForm;

    @Column(name = "batch_size", precision = 18, scale = 3)
    private BigDecimal batchSize;

    @Column(name = "batch_uom", length = 50)
    private String batchUom;

    @Column(name = "hsn_code", length = 50)
    private String hsnCode;

    @Column(name = "therapeutic_category", length = 100)
    private String therapeuticCategory;

    @Column(name = "regulatory_status", length = 50)
    private String regulatoryStatus = "Under Review";

    @Column(name = "shelf_life_value")
    private Integer shelfLifeValue;

    @Column(name = "shelf_life_unit", length = 20)
    private String shelfLifeUnit;

    @Column(name = "storage_condition", length = 100)
    private String storageCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Supplier manufacturer;

    @Column(name = "site_id")
    private Long siteId;

    @Column(name = "production_line_id")
    private Long productionLineId;

    @Column(name = "primary_packaging", length = 200)
    private String primaryPackaging;

    @Column(name = "secondary_packaging", length = 200)
    private String secondaryPackaging;

    @Column(name = "label_template_path", columnDefinition = "TEXT")
    private String labelTemplatePath;

    @Column(name = "sampling_plan", length = 100)
    private String samplingPlan;

    @Column(name = "sample_quantity", precision = 18, scale = 3)
    private BigDecimal sampleQuantity;

    @Column(name = "sample_uom", length = 50)
    private String sampleUom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_reviewer_id")
    private AppUser qcReviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_manager_id")
    private AppUser qcManager;

    @Column(nullable = false, length = 50)
    private String status = "DRAFT";

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(name = "approval_comments", columnDefinition = "TEXT")
    private String approvalComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private AppUser modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
