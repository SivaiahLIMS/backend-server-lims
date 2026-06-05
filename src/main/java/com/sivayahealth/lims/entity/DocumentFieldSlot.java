package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_field_slot",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_case_id", "field_index"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentFieldSlot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

    // References document_version.id (existing entity uses Long id PK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private DocumentTemplateBlock block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Global sequence within the test case (1, 2, 3...)
    @Column(name = "field_index", nullable = false)
    private Integer fieldIndex;

    // Position of this -- occurrence within its block
    @Column(name = "block_local_index", nullable = false)
    private Integer blockLocalIndex = 0;

    // Variable name assigned at parse time: A, B, C... (used in formula_expression)
    @Column(name = "field_variable", nullable = false, length = 10)
    private String fieldVariable;

    // Surrounding text extracted during parse as a hint to the analyst
    @Column(name = "label", columnDefinition = "TEXT")
    private String label;

    // Unit hinted by document text near the -- placeholder
    @Column(name = "default_unit", length = 50)
    private String defaultUnit;

    // Data type of the slot: NUMBER, TEXT, DATE (defaults to NUMBER)
    @Column(name = "field_type", length = 20)
    private String fieldType = "NUMBER";
}
