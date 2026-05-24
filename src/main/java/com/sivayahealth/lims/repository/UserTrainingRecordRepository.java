package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserTrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserTrainingRecordRepository extends JpaRepository<UserTrainingRecord, Long> {
    List<UserTrainingRecord> findByUser_Id(Long userId);
    List<UserTrainingRecord> findByTenantIdAndUser_Id(Long tenantId, Long userId);
    List<UserTrainingRecord> findByTraining_Id(Long trainingId);
    List<UserTrainingRecord> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
}
