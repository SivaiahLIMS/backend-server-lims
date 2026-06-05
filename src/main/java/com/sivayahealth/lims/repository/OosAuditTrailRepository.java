package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.OosAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OosAuditTrailRepository extends JpaRepository<OosAuditTrail, Long> {
    List<OosAuditTrail> findByOosCaseIdOrderByPerformedAtAsc(Long oosCaseId);
}
