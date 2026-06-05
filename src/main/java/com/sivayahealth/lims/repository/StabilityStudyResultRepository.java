package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.StabilityStudyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StabilityStudyResultRepository extends JpaRepository<StabilityStudyResult, Long> {
    List<StabilityStudyResult> findByTimepointId(Long timepointId);
    List<StabilityStudyResult> findByTimepointStudyId(Long studyId);
}
