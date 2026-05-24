package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    List<TaskHistory> findByTask_IdOrderByChangedAtDesc(Long taskId);
}
