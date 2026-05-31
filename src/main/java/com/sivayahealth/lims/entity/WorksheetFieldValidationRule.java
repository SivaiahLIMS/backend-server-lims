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
