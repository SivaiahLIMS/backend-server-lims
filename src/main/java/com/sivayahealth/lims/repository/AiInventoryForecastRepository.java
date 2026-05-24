package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.AiInventoryForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiInventoryForecastRepository extends JpaRepository<AiInventoryForecast, Long> {
    List<AiInventoryForecast> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<AiInventoryForecast> findByTenantIdAndItemTypeAndItemId(Long tenantId, String itemType, Long itemId);
}
