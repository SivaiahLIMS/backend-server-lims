package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMasterRepository extends JpaRepository<DocumentMaster, Long> {
    List<DocumentMaster> findByTenantId(Long tenantId);
    Optional<DocumentMaster> findByTenantIdAndNameAndStatus(Long tenantId, String name, String status);
    List<DocumentMaster> findByTenantIdAndStatus(Long tenantId, String status);
}
