package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SampleTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleTestRepository extends JpaRepository<SampleTest, Long> {
    List<SampleTest> findBySampleId(Long sampleId);
    List<SampleTest> findByAssignedToId(Long userId);
}
