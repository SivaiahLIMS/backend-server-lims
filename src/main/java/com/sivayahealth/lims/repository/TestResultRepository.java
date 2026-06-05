package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findBySampleTestId(Long sampleTestId);
    Optional<TestResult> findFirstBySampleTestIdOrderByEnteredAtDesc(Long sampleTestId);
    List<TestResult> findBySampleTestSampleId(Long sampleId);
}
