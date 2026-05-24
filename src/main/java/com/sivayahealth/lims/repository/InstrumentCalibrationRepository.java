package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentCalibration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentCalibrationRepository extends JpaRepository<InstrumentCalibration, Long> {
    List<InstrumentCalibration> findByInstrumentId(Long instrumentId);
    List<InstrumentCalibration> findByTenantIdAndStatus(Long tenantId, String status);
}
