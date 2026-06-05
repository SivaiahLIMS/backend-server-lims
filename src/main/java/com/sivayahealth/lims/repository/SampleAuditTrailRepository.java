package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SampleAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleAuditTrailRepository extends JpaRepository<SampleAuditTrail, Long> {
    List<SampleAuditTrail> findBySampleIdOrderByPerformedAtDesc(Long sampleId);
}
