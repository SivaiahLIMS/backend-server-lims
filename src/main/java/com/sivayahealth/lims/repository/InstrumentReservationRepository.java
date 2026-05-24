package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstrumentReservationRepository extends JpaRepository<InstrumentReservation, Long> {
    List<InstrumentReservation> findByInstrument_Id(Long instrumentId);
    List<InstrumentReservation> findByInstrument_IdAndStatus(Long instrumentId, String status);
    List<InstrumentReservation> findByWorksheetExecution_Id(Long worksheetExecutionId);
    List<InstrumentReservation> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
}
