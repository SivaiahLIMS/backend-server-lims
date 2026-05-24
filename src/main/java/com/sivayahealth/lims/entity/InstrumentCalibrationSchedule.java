package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "instrument_calibration_schedule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentCalibrationSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private InstrumentMaster instrument;

    @Column(name = "frequency_months", nullable = false)
    private Integer frequencyMonths;

    @Column(name = "tolerance_days", nullable = false)
    private Integer toleranceDays;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "last_calibrated_on")
    private LocalDate lastCalibratedOn;

    @Column(nullable = false, length = 20)
    private String status;
}
