package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "specification",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "test_method_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Specification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "test_method_id")
    private Long testMethodId;

    @Column(name = "min_value", precision = 18, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "target_value", precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "oot_lower", precision = 18, scale = 4)
    private BigDecimal ootLower;

    @Column(name = "oot_upper", precision = 18, scale = 4)
    private BigDecimal ootUpper;

    @Column(name = "oos_lower", precision = 18, scale = 4)
    private BigDecimal oosLower;

    @Column(name = "oos_upper", precision = 18, scale = 4)
    private BigDecimal oosUpper;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
