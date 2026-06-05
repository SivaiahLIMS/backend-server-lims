package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentControlAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentControlAuditTrailRepository extends JpaRepository<DocumentControlAuditTrail, Long> {
    List<DocumentControlAuditTrail> findByDocumentIdOrderByPerformedAtAsc(Long documentId);
}
