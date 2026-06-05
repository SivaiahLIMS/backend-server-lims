package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {
    List<TestExecution> findBySampleTestId(Long sampleTestId);
    Optional<TestExecution> findFirstBySampleTestIdOrderByStartTimeDesc(Long sampleTestId);
}
