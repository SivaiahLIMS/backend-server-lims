package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentDowntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentDowntimeRepository extends JpaRepository<InstrumentDowntime, Long> {
    List<InstrumentDowntime> findByInstrumentId(Long instrumentId);
}
