package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetValidationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorksheetValidationEventRepository
        extends JpaRepository<WorksheetValidationEvent, Long> {

    List<WorksheetValidationEvent> findByWorksheetIdOrderByValidatedAtDesc(Long worksheetId);

    List<WorksheetValidationEvent> findByWorksheetIdAndSlotIdOrderByValidatedAtDesc(
            Long worksheetId, Long slotId);
}
