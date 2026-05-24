package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentCalibrationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentCalibrationResultRepository extends JpaRepository<InstrumentCalibrationResult, Long> {
    List<InstrumentCalibrationResult> findByCalibrationId(Long calibrationId);
}
