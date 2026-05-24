package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TrainingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingMaterialRepository extends JpaRepository<TrainingMaterial, Long> {
    List<TrainingMaterial> findByTenantIdAndBranchIdAndActiveTrue(Long tenantId, Long branchId);
    List<TrainingMaterial> findByTenantIdAndBranchIdAndCategory(Long tenantId, Long branchId, String category);
}
