package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentFieldSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentFieldSlotRepository extends JpaRepository<DocumentFieldSlot, Long> {

    List<DocumentFieldSlot> findByTestCase_TestCaseIdOrderByFieldIndexAsc(Long testCaseId);

    List<DocumentFieldSlot> findByDocumentVersion_IdOrderByTestCase_TestCaseIndexAscFieldIndexAsc(Long documentVersionId);

    void deleteByTestCase_TestCaseId(Long testCaseId);
}
