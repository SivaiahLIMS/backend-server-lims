package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalContainerReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChemicalContainerReservationRepository extends JpaRepository<ChemicalContainerReservation, Long> {
    List<ChemicalContainerReservation> findByContainer_IdAndStatus(Long containerId, String status);
    List<ChemicalContainerReservation> findByWorksheetExecution_Id(Long worksheetExecutionId);
    List<ChemicalContainerReservation> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
}
