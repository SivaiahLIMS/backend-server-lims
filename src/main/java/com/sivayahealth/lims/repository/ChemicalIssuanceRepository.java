package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalIssuance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChemicalIssuanceRepository extends JpaRepository<ChemicalIssuance, Long> {
    List<ChemicalIssuance> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ChemicalIssuance> findByRegistrationId(Long registrationId);
}
