package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "oos_case")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OosCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "case_code", unique = true, length = 30)
    private String caseCode;

    @Column(name = "sample_id")
    private Long sampleId;

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "oos_type", length = 10)
    private String oosType;

    @Column(name = "phase", length = 20)
    private String phase;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by")
    private AppUser raisedBy;

    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt = LocalDateTime.now();

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "root_cause_summary", columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private AppUser closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "oosCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OosNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "oosCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OosActionItem> actionItems = new ArrayList<>();

    @OneToMany(mappedBy = "oosCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OosAuditTrail> auditTrail = new ArrayList<>();
}
