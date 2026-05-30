package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_template_block",
    uniqueConstraints = @UniqueConstraint(columnNames = {"test_case_id", "block_index"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentTemplateBlock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private DocumentTestCase testCase;

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

    // Order within the test case — reconstructs the linked-list on load
    @Column(name = "block_index", nullable = false)
    private Integer blockIndex;

    // PARAGRAPH | TABLE | IMAGE | FORMULA
    @Column(name = "block_type", nullable = false, length = 20)
    private String blockType;

    // PARAGRAPH/FORMULA: {"text": "..."}
    // TABLE: {"rows": [["cell","cell"],["cell","cell"]]}
    // IMAGE: null
    @Column(name = "content_json", columnDefinition = "JSONB")
    private String contentJson;

    // Populated only for IMAGE blocks
    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;
}
