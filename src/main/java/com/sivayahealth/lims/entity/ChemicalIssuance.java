package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chemical_issuance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalIssuance {
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

    @Column(name = "containers_issued", nullable = false)
    private Integer containersIssued;

    @Column(name = "issued_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal issuedQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UomDetails uom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_to")
    private AppUser issuedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private AppUser issuedBy;

    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate = LocalDateTime.now();

    @Column(length = 200)
    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
