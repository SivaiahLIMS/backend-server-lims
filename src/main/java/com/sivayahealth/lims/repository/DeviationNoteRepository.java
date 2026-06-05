package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DeviationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviationNoteRepository extends JpaRepository<DeviationNote, Long> {
    List<DeviationNote> findByDeviationId(Long deviationId);
}
