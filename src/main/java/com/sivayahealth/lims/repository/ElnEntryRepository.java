package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ElnEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElnEntryRepository extends JpaRepository<ElnEntry, Long> {
    List<ElnEntry> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ElnEntry> findByWorksheetExecution_Id(Long worksheetExecutionId);
}
