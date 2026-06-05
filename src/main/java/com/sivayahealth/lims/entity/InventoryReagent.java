package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reagent")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryReagent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "reagent_code", nullable = false, unique = true, length = 50)
    private String reagentCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String category;

    @Column(length = 200)
    private String formula;

    @Column(name = "default_uom", length = 50)
    private String defaultUom;

    @Column(name = "min_stock_level", precision = 18, scale = 4)
    private BigDecimal minStockLevel;

    @Column(name = "reorder_level", precision = 18, scale = 4)
    private BigDecimal reorderLevel;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
