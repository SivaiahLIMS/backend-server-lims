package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worksheet_test_case_result",
    uniqueConstraints = @UniqueConstraint(columnNames = {"worksheet_id", "test_case_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorksheetTestCaseResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_id", nullable = false)
    private WorksheetMaster worksheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Formula with actual values substituted in, e.g. "(12.5 - 0.3) / 15.0 * 100"
    @Column(name = "formula_substituted", columnDefinition = "TEXT")
    private String formulaSubstituted;

    @Column(name = "computed_result", precision = 18, scale = 6)
    private BigDecimal computedResult;

    @Column(name = "result_unit", length = 50)
    private String resultUnit;

    // PASS | FAIL | PENDING
    @Column(name = "pass_fail", nullable = false, length = 20)
    private String passFail = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computed_by")
    private AppUser computedBy;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;
}
