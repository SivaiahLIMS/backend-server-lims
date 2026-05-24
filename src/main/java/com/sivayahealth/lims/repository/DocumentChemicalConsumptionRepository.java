package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentChemicalConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentChemicalConsumptionRepository extends JpaRepository<DocumentChemicalConsumption, Long> {
    List<DocumentChemicalConsumption> findByWorksheetExecution_Id(Long worksheetExecutionId);
    List<DocumentChemicalConsumption> findByContainer_Id(Long containerId);
}
