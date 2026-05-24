package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ChemicalContainerRepository extends JpaRepository<ChemicalContainer, Long> {
    List<ChemicalContainer> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ChemicalContainer> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    Optional<ChemicalContainer> findByTenantIdAndBarcodeValue(Long tenantId, String barcodeValue);

    @Query("SELECT c FROM ChemicalContainer c WHERE c.tenantId = :tenantId AND c.branchId = :branchId AND c.chemical.id = :chemicalId AND c.status = 'AVAILABLE' ORDER BY c.expiryDate ASC NULLS LAST")
    List<ChemicalContainer> findAvailableByChemicalIdOrderByFEFO(Long tenantId, Long branchId, Long chemicalId);
}
