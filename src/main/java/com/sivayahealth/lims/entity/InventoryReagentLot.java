package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reagent_lot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryReagentLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reagent_id", nullable = false)
    private InventoryReagent reagent;

    @Column(name = "lot_number", nullable = false, length = 100)
    private String lotNumber;

    @Column(name = "supplier_lot", length = 100)
    private String supplierLot;

    @Column(name = "received_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal receivedQty;

    @Column(name = "current_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentQty;

    @Column(name = "uom", length = 50)
    private String uom;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "storage_location", length = 200)
    private String storageLocation;

    @Column(name = "certificate_no", length = 100)
    private String certificateNo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
