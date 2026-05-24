package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ContainerStorageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContainerStorageHistoryRepository extends JpaRepository<ContainerStorageHistory, Long> {
    List<ContainerStorageHistory> findByContainerIdOrderByMovedAtDesc(Long containerId);
    List<ContainerStorageHistory> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
