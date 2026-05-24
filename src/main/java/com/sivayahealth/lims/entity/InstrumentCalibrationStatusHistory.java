package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_calibration_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentCalibrationStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_id", nullable = false)
    private InstrumentCalibration calibration;

    @Column(nullable = false, length = 30)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private AppUser changedBy;

    @Column(name = "changed_on", nullable = false)
    private LocalDateTime changedOn = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
