package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstrumentReadingRepository extends JpaRepository<InstrumentReading, Long> {
    List<InstrumentReading> findByInstrument_Id(Long instrumentId);
    List<InstrumentReading> findByCalibrationTask_Id(Long calibrationTaskId);
    List<InstrumentReading> findByWorksheetExecution_Id(Long worksheetExecutionId);
}
