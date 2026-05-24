package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.StorageViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StorageViolationRepository extends JpaRepository<StorageViolation, Long> {
    List<StorageViolation> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<StorageViolation> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<StorageViolation> findByContainerId(Long containerId);
}
