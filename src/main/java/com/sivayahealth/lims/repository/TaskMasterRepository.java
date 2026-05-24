package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TaskMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskMasterRepository extends JpaRepository<TaskMaster, Long> {
    List<TaskMaster> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<TaskMaster> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<TaskMaster> findByAssignee_IdAndStatus(Long assigneeId, String status);
    List<TaskMaster> findByAssignee_Id(Long assigneeId);
    List<TaskMaster> findByRefEntityAndRefId(String refEntity, Long refId);
}
