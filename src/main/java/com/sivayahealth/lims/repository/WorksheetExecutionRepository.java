package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorksheetExecutionRepository extends JpaRepository<WorksheetExecution, Long> {
    List<WorksheetExecution> findByDocumentId(Long documentId);
    List<WorksheetExecution> findByExecutedByIdAndStatus(Long userId, String status);
}
