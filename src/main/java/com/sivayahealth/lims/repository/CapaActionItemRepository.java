package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.CapaActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CapaActionItemRepository extends JpaRepository<CapaActionItem, Long> {
    List<CapaActionItem> findByCapaId(Long capaId);
}
