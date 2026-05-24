package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    List<GoodsReceipt> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<GoodsReceipt> findByPurchaseOrderId(Long poId);
}
