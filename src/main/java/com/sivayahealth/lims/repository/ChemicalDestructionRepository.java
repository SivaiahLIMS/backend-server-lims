package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalDestruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChemicalDestructionRepository extends JpaRepository<ChemicalDestruction, Long> {
    List<ChemicalDestruction> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
