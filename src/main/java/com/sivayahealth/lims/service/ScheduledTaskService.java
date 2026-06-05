package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.AppUserRepository;
import com.sivayahealth.lims.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;

    @Transactional
    public ScheduledTask createTask(Long tenantId, Long branchId, TaskType taskType, String title,
                                    String description, LocalDate dueDate, String recurrenceRule,
                                    Long assignedToUserId, String refEntity, Long refId, Long creatorId) {
        AppUser assignee = null;
        if (assignedToUserId != null) {
            assignee = appUserRepository.findById(assignedToUserId)
                    .orElseThrow(() -> LimsException.notFound("Assigned user not found"));
        }

        ScheduledTask task = ScheduledTask.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .taskType(taskType)
                .title(title)
                .description(description)
                .dueDate(dueDate)
                .recurrenceRule(recurrenceRule)
                .assignedTo(assignee)
                .refEntity(refEntity)
                .refId(refId)
                .status(TaskStatusEnum.PENDING)
                .build();

        ScheduledTask saved = scheduledTaskRepository.save(task);
        auditService.log(tenantId, creatorId, "ScheduledTask", saved.getId(), "CREATE", null, taskType.name());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScheduledTask> getTasks(Long tenantId, Long branchId) {
        return scheduledTaskRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTask> getTasksByStatus(Long tenantId, Long branchId, TaskStatusEnum status) {
        return scheduledTaskRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTask> getTasksByType(Long tenantId, Long branchId, TaskType taskType) {
        return scheduledTaskRepository.findByTenantIdAndBranchIdAndTaskType(tenantId, branchId, taskType);
    }

    @Transactional(readOnly = true)
    public ScheduledTask getTaskById(Long taskId) {
        return scheduledTaskRepository.findById(taskId)
                .orElseThrow(() -> LimsException.notFound("Task not found"));
    }

    @Transactional(readOnly = true)
    public List<ScheduledTask> getMyTasks(Long userId) {
        return scheduledTaskRepository.findByAssignedTo_Id(userId);
    }

    @Transactional
    public ScheduledTask updateStatus(Long taskId, TaskStatusEnum newStatus, String resultNotes, Long userId) {
        ScheduledTask task = scheduledTaskRepository.findById(taskId)
                .orElseThrow(() -> LimsException.notFound("Task not found"));

        TaskStatusEnum old = task.getStatus();
        task.setStatus(newStatus);
        if (resultNotes != null) task.setResultNotes(resultNotes);

        if (newStatus == TaskStatusEnum.COMPLETED || newStatus == TaskStatusEnum.APPROVED) {
            task.setExecutedAt(LocalDateTime.now());
            AppUser executor = new AppUser();
            executor.setId(userId);
            task.setExecutedBy(executor);
        }

        ScheduledTask saved = scheduledTaskRepository.save(task);
        auditService.log(task.getTenantId(), userId, "ScheduledTask", taskId, "STATUS_CHANGE",
                old.name(), newStatus.name());
        return saved;
    }

    @Transactional
    public int runDueTasks(Long tenantId) {
        List<ScheduledTask> dueTasks = scheduledTaskRepository.findDueTasks(tenantId, LocalDate.now());
        for (ScheduledTask task : dueTasks) {
            if (task.getDueDate().isBefore(LocalDate.now())) {
                task.setStatus(TaskStatusEnum.OVERDUE);
            }
        }
        scheduledTaskRepository.saveAll(dueTasks);
        return dueTasks.size();
    }

    @Transactional(readOnly = true)
    public List<ScheduledTask> getOverdueTasks(Long tenantId) {
        return scheduledTaskRepository.findOverdueTasks(tenantId, LocalDate.now());
    }
}
