package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.CalibrationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CalibrationTaskRepository extends JpaRepository<CalibrationTask, Long> {
    List<CalibrationTask> findByInstrument_Id(Long instrumentId);
    List<CalibrationTask> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<CalibrationTask> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    boolean existsByScheduleIdAndStatusIn(Long scheduleId, List<String> statuses);
}
