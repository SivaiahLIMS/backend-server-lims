package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    Optional<ProductSpecification> findByProductProductIdAndTenantIdAndBranchId(
            Long productId, Long tenantId, Long branchId);
}
