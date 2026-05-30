package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTestCaseRepository extends JpaRepository<DocumentTestCase, Long> {

    List<DocumentTestCase> findByDocumentVersion_IdOrderByTestCaseIndexAsc(Long documentVersionId);

    void deleteByDocumentVersion_Id(Long documentVersionId);
}
