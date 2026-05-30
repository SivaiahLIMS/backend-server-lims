package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ProductComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCompositionRepository extends JpaRepository<ProductComposition, Long> {
    List<ProductComposition> findByProductProductIdAndTenantIdAndBranchId(
            Long productId, Long tenantId, Long branchId);
    void deleteByProductProductIdAndTenantIdAndBranchId(
            Long productId, Long tenantId, Long branchId);
}
