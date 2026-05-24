package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.PredictiveAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictiveAlertRepository extends JpaRepository<PredictiveAlert, Long> {
    List<PredictiveAlert> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<PredictiveAlert> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<PredictiveAlert> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
