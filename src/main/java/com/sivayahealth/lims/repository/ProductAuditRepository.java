package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ProductAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAuditRepository extends JpaRepository<ProductAudit, Long> {
    List<ProductAudit> findByProductProductIdAndTenantIdAndBranchIdOrderByChangedAtDesc(
            Long productId, Long tenantId, Long branchId);
}
