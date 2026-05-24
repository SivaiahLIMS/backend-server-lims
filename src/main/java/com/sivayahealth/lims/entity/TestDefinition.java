package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "test_definition")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "method_id")
    private Long methodId;

    @Column(length = 100)
    private String matrix;

    @Column(length = 50)
    private String unit;

    @Column(name = "spec_min", precision = 18, scale = 4)
    private BigDecimal specMin;

    @Column(name = "spec_max", precision = 18, scale = 4)
    private BigDecimal specMax;

    @Column(nullable = false, length = 20)
    private String status;
}
