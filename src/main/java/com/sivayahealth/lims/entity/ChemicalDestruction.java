package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chemical_destruction")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalDestruction {
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

    @Column(name = "containers_destroyed", nullable = false)
    private Integer containersDestroyed;

    @Column(name = "quantity_destroyed", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDestroyed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UomDetails uom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destroyed_by")
    private AppUser destroyedBy;

    @Column(name = "destruction_date", nullable = false)
    private LocalDateTime destructionDate = LocalDateTime.now();

    @Column(length = 200)
    private String method;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "witnessed_by")
    private AppUser witnessedBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
