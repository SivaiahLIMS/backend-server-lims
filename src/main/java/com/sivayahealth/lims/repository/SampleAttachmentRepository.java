package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SampleAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleAttachmentRepository extends JpaRepository<SampleAttachment, Long> {
    List<SampleAttachment> findBySampleId(Long sampleId);
}
