package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chemical_container_reservation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalContainerReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private ChemicalContainer container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_execution_id")
    private WorksheetExecution worksheetExecution;

    @Column(name = "reserved_qty", nullable = false)
    private BigDecimal reservedQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private UomDetails uom;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by")
    private AppUser reservedBy;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;
}
