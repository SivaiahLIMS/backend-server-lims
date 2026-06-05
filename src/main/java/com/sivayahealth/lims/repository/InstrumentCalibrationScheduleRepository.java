package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentCalibrationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentCalibrationScheduleRepository extends JpaRepository<InstrumentCalibrationSchedule, Long> {
    Optional<InstrumentCalibrationSchedule> findByInstrumentId(Long instrumentId);
    List<InstrumentCalibrationSchedule> findByStatus(String status);
    List<InstrumentCalibrationSchedule> findByNextDueDateLessThanEqualAndStatus(LocalDate date, String status);
    List<InstrumentCalibrationSchedule> findByNextDueDateLessThanAndStatus(LocalDate date, String status);
}
