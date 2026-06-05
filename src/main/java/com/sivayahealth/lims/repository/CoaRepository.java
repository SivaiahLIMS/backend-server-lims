package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Coa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CoaRepository extends JpaRepository<Coa, Long> {
    Optional<Coa> findBySampleId(Long sampleId);
    List<Coa> findByTenantId(Long tenantId);
    List<Coa> findByTenantIdAndStatus(Long tenantId, String status);
    List<Coa> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
