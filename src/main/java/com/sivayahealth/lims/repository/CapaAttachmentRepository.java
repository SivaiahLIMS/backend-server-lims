package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.CapaAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CapaAttachmentRepository extends JpaRepository<CapaAttachment, Long> {
    List<CapaAttachment> findByCapaId(Long capaId);
}
