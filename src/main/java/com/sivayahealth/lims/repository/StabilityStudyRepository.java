package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.StabilityStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StabilityStudyRepository extends JpaRepository<StabilityStudy, Long> {
    List<StabilityStudy> findByTenantId(Long tenantId);
    List<StabilityStudy> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<StabilityStudy> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<StabilityStudy> findByProduct_ProductId(Long productId);
}
