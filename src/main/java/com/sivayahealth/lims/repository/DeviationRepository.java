package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Deviation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviationRepository extends JpaRepository<Deviation, Long> {
    List<Deviation> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<Deviation> findByTenantIdAndStatus(Long tenantId, String status);
}
