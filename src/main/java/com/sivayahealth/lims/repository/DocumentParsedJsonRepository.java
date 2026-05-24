package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentParsedJson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentParsedJsonRepository extends JpaRepository<DocumentParsedJson, Long> {
    Optional<DocumentParsedJson> findByDocumentIdAndVersion(Long documentId, Integer version);
    Optional<DocumentParsedJson> findByDocument_IdAndVersion(Long documentId, Integer version);
}
