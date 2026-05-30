package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_composition")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductComposition {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "composition_id")
    private Long compositionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductMaster product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private ChemicalMaster ingredient;

    @Column(name = "ingredient_quantity", precision = 18, scale = 3)
    private BigDecimal ingredientQuantity;

    @Column(name = "ingredient_uom", length = 50)
    private String ingredientUom;

    @Column(name = "ingredient_grade", length = 50)
    private String ingredientGrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
