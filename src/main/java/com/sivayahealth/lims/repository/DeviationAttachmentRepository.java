package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DeviationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviationAttachmentRepository extends JpaRepository<DeviationAttachment, Long> {
    List<DeviationAttachment> findByDeviationId(Long deviationId);
}
