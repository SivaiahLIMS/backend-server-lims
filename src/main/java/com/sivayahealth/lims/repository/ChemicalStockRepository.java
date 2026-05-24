package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChemicalStockRepository extends JpaRepository<ChemicalStock, Long> {
    Optional<ChemicalStock> findByRegistrationId(Long registrationId);
    List<ChemicalStock> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ChemicalStock> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
}
