package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_method")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestMethod {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sop_document_id")
    private Long sopDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id")
    private Specification specification;

    @Column(length = 50)
    private String version;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
