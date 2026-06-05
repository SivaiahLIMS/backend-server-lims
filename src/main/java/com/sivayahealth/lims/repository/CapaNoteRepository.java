package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.CapaNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CapaNoteRepository extends JpaRepository<CapaNote, Long> {
    List<CapaNote> findByCapaId(Long capaId);
}
