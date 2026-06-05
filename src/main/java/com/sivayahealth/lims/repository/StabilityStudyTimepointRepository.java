package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.StabilityStudyTimepoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StabilityStudyTimepointRepository extends JpaRepository<StabilityStudyTimepoint, Long> {
    List<StabilityStudyTimepoint> findByStudyId(Long studyId);
    List<StabilityStudyTimepoint> findByStudyIdAndStatus(Long studyId, String status);
    List<StabilityStudyTimepoint> findByStatusAndScheduledDateBefore(String status, LocalDate date);
}
