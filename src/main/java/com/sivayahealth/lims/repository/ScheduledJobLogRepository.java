package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ScheduledJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduledJobLogRepository extends JpaRepository<ScheduledJobLog, Long> {
    List<ScheduledJobLog> findByJobNameOrderByStartedAtDesc(String jobName);
    List<ScheduledJobLog> findTop20ByOrderByStartedAtDesc();
}
