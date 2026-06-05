package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_test_id", nullable = false)
    private SampleTest sampleTest;

    @Column(name = "parameter_name", length = 200)
    private String parameterName;

    @Column(name = "result_value", length = 200)
    private String resultValue;

    @Column(name = "numeric_value", precision = 18, scale = 4)
    private BigDecimal numericValue;

    @Column(length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultQualifier qualifier;

    @Column(name = "oos_flag")
    @Builder.Default
    private Boolean oosFlag = false;

    @Column(name = "oot_flag")
    @Builder.Default
    private Boolean ootFlag = false;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ENTERED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by")
    private AppUser enteredBy;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
