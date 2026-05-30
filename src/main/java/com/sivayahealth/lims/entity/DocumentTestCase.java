package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_test_case",
    uniqueConstraints = @UniqueConstraint(columnNames = {"document_version_id", "test_case_index"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentTestCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_case_id")
    private Long testCaseId;

    // References document_version.id (existing entity uses Long id PK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "test_case_index", nullable = false)
    private Integer testCaseIndex;

    @Column(name = "test_case_name", length = 500)
    private String testCaseName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "formula_text", nullable = false, columnDefinition = "TEXT")
    private String formulaText;

    // Parseable form: -- replaced with variable references A, B, C...
    @Column(name = "formula_expression", columnDefinition = "TEXT")
    private String formulaExpression;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
