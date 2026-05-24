package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chemical_container")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemical_id", nullable = false)
    private ChemicalMaster chemical;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private ChemicalRegistration registration;

    @Column(name = "container_code", nullable = false)
    private String containerCode;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private UomDetails uom;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "barcode_value")
    private String barcodeValue;

    @Column(name = "lot_no")
    private String lotNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
