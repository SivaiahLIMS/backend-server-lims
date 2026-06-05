package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_control")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "document_code", nullable = false, length = 50)
    private String documentCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "doc_type", nullable = false, length = 20)
    private String docType;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "review_due_date")
    private LocalDate reviewDueDate;

    @Column(name = "review_period_months")
    private Integer reviewPeriodMonths;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentControlVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentControlAuditTrail> auditTrail = new ArrayList<>();
}
