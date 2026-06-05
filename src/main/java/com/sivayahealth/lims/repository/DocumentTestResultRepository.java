package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentTestResultRepository extends JpaRepository<DocumentTestResult, Long> {
    List<DocumentTestResult> findByWorksheetExecution_Id(Long worksheetExecutionId);
    List<DocumentTestResult> findByWorksheetExecution_IdAndOosTrue(Long worksheetExecutionId);
    List<DocumentTestResult> findByTenantIdAndBranchIdAndOosTrue(Long tenantId, Long branchId);
    List<DocumentTestResult> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
