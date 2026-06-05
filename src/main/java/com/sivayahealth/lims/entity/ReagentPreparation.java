package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reagent_preparation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReagentPreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private ChemicalRegistration registration;

    @Column(name = "prep_no", unique = true, length = 50)
    private String prepNo;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String formula;

    @Column(length = 100)
    private String concentration;

    @Column(name = "volume_prepared", precision = 18, scale = 4)
    private BigDecimal volumePrepared;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private UomDetails uom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by")
    private AppUser preparedBy;

    @Column(name = "prepared_at", nullable = false)
    private LocalDateTime preparedAt = LocalDateTime.now();

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
