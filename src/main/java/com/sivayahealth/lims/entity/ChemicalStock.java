package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chemical_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private ChemicalRegistration registration;

    @Column(name = "containers_in_stock", nullable = false)
    private Integer containersInStock;

    @Column(name = "quantity_in_stock", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityInStock;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();
}
