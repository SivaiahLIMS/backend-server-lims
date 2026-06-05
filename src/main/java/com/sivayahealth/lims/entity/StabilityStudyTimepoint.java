package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stability_study_timepoint")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StabilityStudyTimepoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private StabilityStudy study;

    @Column(nullable = false, length = 30)
    private String timepoint;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "completed_by")
    private Long completedBy;

    @OneToMany(mappedBy = "timepoint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StabilityStudyResult> results = new ArrayList<>();
}
