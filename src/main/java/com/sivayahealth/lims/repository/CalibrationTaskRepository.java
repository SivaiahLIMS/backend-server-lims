package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.CalibrationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalibrationTaskRepository extends JpaRepository<CalibrationTask, Long> {
    List<CalibrationTask> findByInstrument_Id(Long instrumentId);
    List<CalibrationTask> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<CalibrationTask> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
