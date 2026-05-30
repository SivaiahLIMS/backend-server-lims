package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorksheetReviewHistoryRepository extends JpaRepository<WorksheetReviewHistory, Long> {
    List<WorksheetReviewHistory> findByWorksheet_WorksheetIdAndTenantIdAndBranchIdOrderByActionAtAsc(
            Long worksheetId, Long tenantId, Long branchId);
}
