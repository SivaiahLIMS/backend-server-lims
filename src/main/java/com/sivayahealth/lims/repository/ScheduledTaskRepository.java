package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ScheduledTask;
import com.sivayahealth.lims.entity.TaskStatusEnum;
import com.sivayahealth.lims.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    List<ScheduledTask> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ScheduledTask> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, TaskStatusEnum status);
    List<ScheduledTask> findByTenantIdAndBranchIdAndTaskType(Long tenantId, Long branchId, TaskType taskType);
    List<ScheduledTask> findByAssignedTo_Id(Long userId);

    @Query("SELECT t FROM ScheduledTask t WHERE t.tenantId = :tenantId AND t.status = 'PENDING' AND t.dueDate <= :date")
    List<ScheduledTask> findDueTasks(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT t FROM ScheduledTask t WHERE t.tenantId = :tenantId AND t.status = 'PENDING' AND t.dueDate < :date")
    List<ScheduledTask> findOverdueTasks(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    List<ScheduledTask> findByRefEntityAndRefId(String refEntity, Long refId);
}
