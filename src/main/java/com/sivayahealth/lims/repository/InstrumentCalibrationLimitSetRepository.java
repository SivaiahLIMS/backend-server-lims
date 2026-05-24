package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentCalibrationLimitSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InstrumentCalibrationLimitSetRepository extends JpaRepository<InstrumentCalibrationLimitSet, Long> {
    List<InstrumentCalibrationLimitSet> findByInstrument_Id(Long instrumentId);
    Optional<InstrumentCalibrationLimitSet> findByInstrument_IdAndActiveTrue(Long instrumentId);
}
