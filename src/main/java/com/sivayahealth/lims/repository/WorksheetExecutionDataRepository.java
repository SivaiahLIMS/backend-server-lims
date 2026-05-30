package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetExecutionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorksheetExecutionDataRepository extends JpaRepository<WorksheetExecutionData, Long> {
    List<WorksheetExecutionData> findByWorksheet_WorksheetIdAndTenantIdAndBranchId(
            Long worksheetId, Long tenantId, Long branchId);
    void deleteByWorksheet_WorksheetIdAndTenantIdAndBranchId(
            Long worksheetId, Long tenantId, Long branchId);
}
