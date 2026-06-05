package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentControlAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentControlAttachmentRepository extends JpaRepository<DocumentControlAttachment, Long> {
    List<DocumentControlAttachment> findByVersionId(Long versionId);
}
