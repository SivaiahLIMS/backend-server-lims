package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DeviationAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviationAuditTrailRepository extends JpaRepository<DeviationAuditTrail, Long> {
    List<DeviationAuditTrail> findByDeviationIdOrderByPerformedAtAsc(Long deviationId);
}
