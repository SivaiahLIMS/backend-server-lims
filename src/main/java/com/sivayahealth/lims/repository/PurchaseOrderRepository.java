package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<PurchaseOrder> findByTenantIdAndStatus(Long tenantId, String status);
}
