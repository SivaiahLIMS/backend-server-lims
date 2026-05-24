package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleRepository extends JpaRepository<Sample, Long> {
    List<Sample> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<Sample> findByTenantIdAndStatus(Long tenantId, String status);
}
