package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMasterRepository taskMasterRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final UserWorkloadRepository userWorkloadRepository;
    private final AppUserRepository appUserRepository;

    public List<TaskMaster> getTasks(Long tenantId, Long branchId) {
        return taskMasterRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    public List<TaskMaster> getTasksByStatus(Long tenantId, Long branchId, String status) {
        return taskMasterRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
    }

    public List<TaskMaster> getMyTasks(Long userId) {
        return taskMasterRepository.findByAssignee_Id(userId);
    }

    public TaskMaster getTask(Long id) {
        return taskMasterRepository.findById(id)
                .orElseThrow(() ->  LimsException.notFound("Task not found: "));
    }

    @Transactional
    public TaskMaster createTask(TaskMaster task) {
        TaskMaster saved = taskMasterRepository.save(task);
        recordHistory(saved, null, "CREATED", null, null);
        if (task.getAssignee() != null) {
            incrementWorkload(task.getTenantId(), task.getBranchId(), task.getAssignee().getId());
        }
        return saved;
    }

    @Transactional
    public TaskMaster acceptTask(Long id, Long userId) {
        TaskMaster task = getTask(id);
        String old = task.getStatus();
        task.setStatus("ACCEPTED");
        task.setAcceptedAt(LocalDateTime.now());
        TaskMaster saved = taskMasterRepository.save(task);
        recordHistory(saved, old, "ACCEPTED", userId, null);
        return saved;
    }

    @Transactional
    public TaskMaster startTask(Long id, Long userId) {
        TaskMaster task = getTask(id);
        String old = task.getStatus();
        task.setStatus("IN_PROGRESS");
        TaskMaster saved = taskMasterRepository.save(task);
        recordHistory(saved, old, "IN_PROGRESS", userId, null);
        return saved;
    }

    @Transactional
    public TaskMaster completeTask(Long id, Long userId, String comment) {
        TaskMaster task = getTask(id);
        String old = task.getStatus();
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        TaskMaster saved = taskMasterRepository.save(task);
        recordHistory(saved, old, "COMPLETED", userId, comment);
        if (task.getAssignee() != null) {
            decrementWorkload(task.getTenantId(), task.getBranchId(), task.getAssignee().getId());
        }
        return saved;
    }

    @Transactional
    public TaskMaster approveTask(Long id, Long userId, String comment) {
        TaskMaster task = getTask(id);
        String old = task.getStatus();
        AppUser approver = appUserRepository.findById(userId).orElse(null);
        task.setStatus("APPROVED");
        task.setApprovedBy(approver);
        task.setApprovedAt(LocalDateTime.now());
        TaskMaster saved = taskMasterRepository.save(task);
        recordHistory(saved, old, "APPROVED", userId, comment);
        return saved;
    }

    @Transactional
    public TaskMaster rejectTask(Long id, Long userId, String comment) {
        TaskMaster task = getTask(id);
        String old = task.getStatus();
        task.setStatus("REJECTED");
        TaskMaster saved = taskMasterRepository.save(task);
        recordHistory(saved, old, "REJECTED", userId, comment);
        return saved;
    }

    public List<TaskHistory> getTaskHistory(Long taskId) {
        return taskHistoryRepository.findByTask_IdOrderByChangedAtDesc(taskId);
    }

    private void recordHistory(TaskMaster task, String oldStatus, String newStatus, Long changedById, String comment) {
        AppUser changedBy = changedById != null ? appUserRepository.findById(changedById).orElse(null) : null;
        TaskHistory history = TaskHistory.builder()
                .tenantId(task.getTenantId())
                .branchId(task.getBranchId())
                .task(task)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .comment(comment)
                .build();
        taskHistoryRepository.save(history);
    }

    private void incrementWorkload(Long tenantId, Long branchId, Long userId) {
        userWorkloadRepository.findByTenantIdAndUser_Id(tenantId, userId).ifPresent(w -> {
            w.setOpenTasks(w.getOpenTasks() + 1);
            w.setUpdatedAt(LocalDateTime.now());
            userWorkloadRepository.save(w);
        });
    }

    private void decrementWorkload(Long tenantId, Long branchId, Long userId) {
        userWorkloadRepository.findByTenantIdAndUser_Id(tenantId, userId).ifPresent(w -> {
            w.setOpenTasks(Math.max(0, w.getOpenTasks() - 1));
            w.setUpdatedAt(LocalDateTime.now());
            userWorkloadRepository.save(w);
        });
    }
}
