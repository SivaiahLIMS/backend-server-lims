package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deviation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Deviation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "deviation_code", unique = true, length = 30)
    private String deviationCode;

    @Column(name = "ref_entity", length = 50)
    private String refEntity;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String severity;

    @Column(name = "deviation_type", length = 20)
    private String deviationType;

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

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private AppUser closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closure_remarks", columnDefinition = "TEXT")
    private String closureRemarks;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "deviation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviationNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "deviation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviationActionItem> actionItems = new ArrayList<>();

    @OneToMany(mappedBy = "deviation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviationAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "deviation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviationAuditTrail> auditTrail = new ArrayList<>();
}
