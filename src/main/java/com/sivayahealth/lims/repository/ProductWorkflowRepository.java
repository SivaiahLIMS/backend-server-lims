package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ProductWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductWorkflowRepository extends JpaRepository<ProductWorkflow, Long> {
    List<ProductWorkflow> findByProductProductIdAndTenantIdAndBranchIdOrderByActionAtAsc(
            Long productId, Long tenantId, Long branchId);
}
