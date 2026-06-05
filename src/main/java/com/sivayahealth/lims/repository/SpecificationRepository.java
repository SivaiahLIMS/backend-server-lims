package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, Long> {
    List<Specification> findByTenantIdAndProductId(Long tenantId, Long productId);
    Optional<Specification> findByTenantIdAndProductIdAndTestMethodId(Long tenantId, Long productId, Long testMethodId);
    List<Specification> findByTenantIdAndActiveTrue(Long tenantId);
}
