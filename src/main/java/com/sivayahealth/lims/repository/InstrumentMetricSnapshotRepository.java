package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface InstrumentMetricSnapshotRepository extends JpaRepository<InstrumentMetricSnapshot, Long> {
    List<InstrumentMetricSnapshot> findByInstrument_IdOrderByMetricDateAsc(Long instrumentId);
    List<InstrumentMetricSnapshot> findByInstrument_IdAndMetricTypeAndMetricDateBetween(Long instrumentId, String metricType, LocalDate from, LocalDate to);
}
