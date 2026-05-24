package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "instrument_maintenance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentMaintenance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private InstrumentMaster instrument;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(length = 100)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Supplier vendor;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, length = 20)
    private String status;
}
