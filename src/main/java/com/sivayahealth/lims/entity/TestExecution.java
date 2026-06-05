package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_execution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestExecution {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_test_id", nullable = false)
    private SampleTest sampleTest;

    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "execution_data_json", columnDefinition = "TEXT")
    private String executionDataJson;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by")
    private AppUser executedBy;
}
