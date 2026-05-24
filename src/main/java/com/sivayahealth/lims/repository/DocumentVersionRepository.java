package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocument_Id(Long documentId);
    Optional<DocumentVersion> findByDocument_IdAndVersionNo(Long documentId, int versionNo);
    List<DocumentVersion> findByDocument_IdAndLifecycleState(Long documentId, String lifecycleState);
}
