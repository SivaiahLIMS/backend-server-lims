package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InventoryReagent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReagentRepository extends JpaRepository<InventoryReagent, Long> {
    List<InventoryReagent> findByTenantId(Long tenantId);
    List<InventoryReagent> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    Optional<InventoryReagent> findByReagentCode(String reagentCode);
    List<InventoryReagent> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
}
