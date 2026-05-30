package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorksheetFieldValueRepository extends JpaRepository<WorksheetFieldValue, Long> {

    List<WorksheetFieldValue> findByWorksheet_WorksheetId(Long worksheetId);

    List<WorksheetFieldValue> findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
            Long worksheetId, Long testCaseId);

    Optional<WorksheetFieldValue> findByWorksheet_WorksheetIdAndSlot_SlotId(
            Long worksheetId, Long slotId);

    void deleteByWorksheet_WorksheetId(Long worksheetId);
}
