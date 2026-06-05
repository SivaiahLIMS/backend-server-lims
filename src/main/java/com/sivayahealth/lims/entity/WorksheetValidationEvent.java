package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_validation_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetValidationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worksheet_id")
    private Long worksheetId;

    @Column(name = "slot_id")
    private Long slotId;

    private BigDecimal value;

    private String unit;

    private String status;

    private String severity;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "requires_comment")
    private boolean requiresComment;

    @Column(name = "requires_review")
    private boolean requiresReview;

    @Column(name = "requires_investigation")
    private boolean requiresInvestigation;

    @Column(name = "requires_capa")
    private boolean requiresCapa;

    /** FIELD_BLUR — textbox on-blur; COMPUTED_RESULT — after formula computation */
    @Column(name = "source")
    private String source = "FIELD_BLUR";

    @Column(name = "validated_by")
    private Long validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
}
