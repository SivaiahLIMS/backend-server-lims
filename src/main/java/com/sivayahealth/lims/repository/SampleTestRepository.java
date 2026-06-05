package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SampleTest;
import com.sivayahealth.lims.entity.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleTestRepository extends JpaRepository<SampleTest, Long> {
    List<SampleTest> findBySampleId(Long sampleId);
    List<SampleTest> findByAssignedToId(Long userId);
    boolean existsBySampleIdAndStatusNot(Long sampleId, TestStatus status);
    List<SampleTest> findBySampleIdAndStatus(Long sampleId, TestStatus status);
}
