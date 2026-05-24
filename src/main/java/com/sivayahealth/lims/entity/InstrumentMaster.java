package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "instrument_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentMaster {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InstrumentCategory category;

    @Column(name = "instrument_code", unique = true, nullable = false, length = 100)
    private String instrumentCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "serial_no", length = 200)
    private String serialNo;

    @Column(length = 200)
    private String model;

    @Column(length = 200)
    private String make;

    @Column(name = "installed_at", length = 200)
    private String installedAt;

    @Column(name = "installed_on")
    private LocalDate installedOn;

    @Column(length = 200)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "warranty_expiry")
    private LocalDate warrantyExpiry;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "barcode_value", length = 200)
    private String barcodeValue;
}
