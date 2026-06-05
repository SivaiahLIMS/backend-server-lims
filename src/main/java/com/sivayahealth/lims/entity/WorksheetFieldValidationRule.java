package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_field_validation_rule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetFieldValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private DocumentFieldSlot slot;

    @Column(name = "field_type")
    private String fieldType = "NUMBER";

    /**
     * RANGE        — both lower and upper OOS limits apply (e.g. Assay 98.0–102.0%)
     * MIN_ONLY     — only lower limit applies (e.g. Dissolution ≥ 80%)
     * MAX_ONLY     — only upper limit applies (e.g. Impurity ≤ 0.5%)
     * TARGET_VARIANCE — value must be within ± variance of a target
     * FORMULA_BASED   — rule evaluated via expression, not simple bounds
     */
    @Column(name = "rule_type")
    private String ruleType = "RANGE";

    private String unit;

    @Column(name = "oos_lower_limit")
    private BigDecimal oosLowerLimit;

    @Column(name = "oos_upper_limit")
    private BigDecimal oosUpperLimit;

    @Column(name = "oot_lower_limit")
    private BigDecimal ootLowerLimit;

    @Column(name = "oot_upper_limit")
    private BigDecimal ootUpperLimit;

    @Column(name = "require_comment_on_oos")
    private boolean requireCommentOnOos = true;

    @Column(name = "require_comment_on_oot")
    private boolean requireCommentOnOot = true;

    @Column(name = "require_review_on_oot")
    private boolean requireReviewOnOot = false;

    @Column(name = "require_investigation_on_oos")
    private boolean requireInvestigationOnOos = false;

    @Column(name = "require_capa_on_oos")
    private boolean requireCapaOnOos = false;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
