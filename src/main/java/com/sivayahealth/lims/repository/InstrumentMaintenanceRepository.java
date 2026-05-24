package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentMaintenanceRepository extends JpaRepository<InstrumentMaintenance, Long> {
    List<InstrumentMaintenance> findByInstrumentId(Long instrumentId);
}
