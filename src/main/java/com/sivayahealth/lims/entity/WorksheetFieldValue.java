package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_field_value",
    uniqueConstraints = @UniqueConstraint(columnNames = {"worksheet_id", "slot_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetFieldValue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "value_id")
    private Long valueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_id", nullable = false)
    private WorksheetMaster worksheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private DocumentFieldSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Textbox input — analyst measurement
    @Column(name = "numeric_value", precision = 18, scale = 6)
    private BigDecimal numericValue;

    // Dropdown 1: ml / L / g / kg / mg / µg / mEq / IU / %
    @Column(name = "unit", length = 50)
    private String unit;

    // Dropdown 2: EXACT / APPROX / TRACE / ND
    @Column(name = "qualifier", length = 50)
    private String qualifier = "EXACT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by")
    private AppUser enteredBy;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private AppUser modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
