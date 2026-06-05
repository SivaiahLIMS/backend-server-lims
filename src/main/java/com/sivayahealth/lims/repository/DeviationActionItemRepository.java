package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DeviationActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviationActionItemRepository extends JpaRepository<DeviationActionItem, Long> {
    List<DeviationActionItem> findByDeviationId(Long deviationId);
}
