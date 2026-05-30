package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentTemplateBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTemplateBlockRepository extends JpaRepository<DocumentTemplateBlock, Long> {

    List<DocumentTemplateBlock> findByTestCase_TestCaseIdOrderByBlockIndexAsc(Long testCaseId);

    List<DocumentTemplateBlock> findByDocumentVersion_IdOrderByTestCase_TestCaseIndexAscBlockIndexAsc(Long documentVersionId);

    void deleteByTestCase_TestCaseId(Long testCaseId);
}
