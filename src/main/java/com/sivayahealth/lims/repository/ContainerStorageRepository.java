package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ContainerStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ContainerStorageRepository extends JpaRepository<ContainerStorage, Long> {
    Optional<ContainerStorage> findByContainerId(Long containerId);
    List<ContainerStorage> findByLocation_Id(Long locationId);
    List<ContainerStorage> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
