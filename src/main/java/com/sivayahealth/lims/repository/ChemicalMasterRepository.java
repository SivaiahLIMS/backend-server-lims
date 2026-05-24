package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChemicalMasterRepository extends JpaRepository<ChemicalMaster, Long> {
    List<ChemicalMaster> findByTenantIdAndActiveTrue(Long tenantId);
    boolean existsByCasNoAndTenantId(String casNo, Long tenantId);
}
