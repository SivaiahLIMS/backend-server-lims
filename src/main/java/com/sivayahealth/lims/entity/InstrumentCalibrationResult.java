package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "instrument_calibration_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentCalibrationResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_id", nullable = false)
    private InstrumentCalibration calibration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private InstrumentTestTemplate template;

    @Column(precision = 18, scale = 4)
    private BigDecimal observation;

    @Column(name = "acquired_time", nullable = false)
    private LocalDateTime acquiredTime = LocalDateTime.now();

    @Column(name = "pass_fail", length = 10)
    private String passFail;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
