package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentCalibrationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentCalibrationStatusHistoryRepository extends JpaRepository<InstrumentCalibrationStatusHistory, Long> {
    List<InstrumentCalibrationStatusHistory> findByCalibrationIdOrderByChangedOnDesc(Long calibrationId);
}
