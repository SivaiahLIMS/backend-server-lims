package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ReleaseDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseDecisionRepository extends JpaRepository<ReleaseDecision, Long> {
    Optional<ReleaseDecision> findBySampleId(Long sampleId);
    List<ReleaseDecision> findByDecision(String decision);
}
