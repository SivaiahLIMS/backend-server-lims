package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "release_decision")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReleaseDecision {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReleaseStatus decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by", nullable = false)
    private AppUser decidedBy;

    @Column(name = "decided_at", nullable = false)
    @Builder.Default
    private LocalDateTime decidedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String reason;
}
