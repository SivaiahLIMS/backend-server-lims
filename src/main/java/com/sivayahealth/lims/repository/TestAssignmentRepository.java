package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestAssignmentRepository extends JpaRepository<TestAssignment, Long> {
    Optional<TestAssignment> findBySampleTestId(Long sampleTestId);
    List<TestAssignment> findByAnalystId(Long analystId);
}
