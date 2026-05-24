package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "instrument_test_template")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentTestTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_category_id")
    private InstrumentCategory instrumentCategory;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "spec_min", precision = 18, scale = 4)
    private BigDecimal specMin;

    @Column(name = "spec_max", precision = 18, scale = 4)
    private BigDecimal specMax;

    @Column(length = 50)
    private String uom;

    @Column(nullable = false)
    private Boolean active = true;
}
