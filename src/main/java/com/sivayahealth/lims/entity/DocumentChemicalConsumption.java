package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_chemical_consumption")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentChemicalConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worksheet_execution_id", nullable = false)
    private WorksheetExecution worksheetExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private ChemicalContainer container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private ChemicalContainerReservation reservation;

    @Column(name = "consumed_qty", nullable = false)
    private BigDecimal consumedQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private UomDetails uom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumed_by")
    private AppUser consumedBy;

    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt = LocalDateTime.now();
}
