package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.OosNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OosNoteRepository extends JpaRepository<OosNote, Long> {
    List<OosNote> findByOosCaseId(Long oosCaseId);
}
