package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Capa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deviation_id")
    private Deviation deviation;

    @Column(name = "capa_code", length = 30)
    private String capaCode;

    @Column(length = 200)
    private String title;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "action_desc", nullable = false, columnDefinition = "TEXT")
    private String actionDesc;

    @Column(length = 20)
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "closure_remarks", columnDefinition = "TEXT")
    private String closureRemarks;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "capa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CapaNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "capa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CapaActionItem> actionItems = new ArrayList<>();

    @OneToMany(mappedBy = "capa", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CapaAttachment> attachments = new ArrayList<>();
}
