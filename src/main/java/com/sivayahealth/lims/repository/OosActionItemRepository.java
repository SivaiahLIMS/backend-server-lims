package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.OosActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OosActionItemRepository extends JpaRepository<OosActionItem, Long> {
    List<OosActionItem> findByOosCaseId(Long oosCaseId);
}
