package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stability_study_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StabilityStudyResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timepoint_id", nullable = false)
    private StabilityStudyTimepoint timepoint;

    @Column(nullable = false, length = 200)
    private String parameter;

    @Column(length = 200)
    private String specification;

    @Column(length = 200)
    private String result;

    @Column(name = "pass_fail", length = 10)
    private String passFail;

    @Column(name = "tested_by")
    private Long testedBy;

    @Column(name = "tested_at")
    private LocalDateTime testedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
