package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentControlVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentControlVersionRepository extends JpaRepository<DocumentControlVersion, Long> {
    List<DocumentControlVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
}
