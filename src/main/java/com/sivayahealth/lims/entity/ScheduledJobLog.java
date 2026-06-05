package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_job_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledJobLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(columnDefinition = "TEXT")
    private String message;
}
