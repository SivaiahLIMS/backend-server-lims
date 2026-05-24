package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
    List<StorageLocation> findByTenantIdAndBranchIdAndActiveTrue(Long tenantId, Long branchId);
    List<StorageLocation> findByTenantIdAndBranchIdAndParentIdIsNull(Long tenantId, Long branchId);
    List<StorageLocation> findByTenantIdAndBranchIdAndParentId(Long tenantId, Long branchId, Long parentId);
    java.util.Optional<StorageLocation> findByTenantIdAndBranchIdAndCode(Long tenantId, Long branchId, String code);
}
