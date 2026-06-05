package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sample_type")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SampleType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "sample_type_default_test",
            joinColumns = @JoinColumn(name = "sample_type_id"))
    @Column(name = "test_method_id")
    @Builder.Default
    private List<Long> defaultTestMethodIds = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
