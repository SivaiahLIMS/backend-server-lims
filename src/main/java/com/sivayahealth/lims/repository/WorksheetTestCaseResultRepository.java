package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetTestCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorksheetTestCaseResultRepository extends JpaRepository<WorksheetTestCaseResult, Long> {

    List<WorksheetTestCaseResult> findByWorksheet_WorksheetIdOrderByTestCase_TestCaseIndexAsc(
            Long worksheetId);

    Optional<WorksheetTestCaseResult> findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
            Long worksheetId, Long testCaseId);

    void deleteByWorksheet_WorksheetId(Long worksheetId);
}
