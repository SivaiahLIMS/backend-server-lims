package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.OosCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OosCaseRepository extends JpaRepository<OosCase, Long> {
    List<OosCase> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<OosCase> findByTenantIdAndStatus(Long tenantId, String status);
    List<OosCase> findByTenantId(Long tenantId);
}
