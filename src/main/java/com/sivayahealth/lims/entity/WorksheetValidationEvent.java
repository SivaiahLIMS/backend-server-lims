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

    @Column(name = "validated_by")
    private Long validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
}
