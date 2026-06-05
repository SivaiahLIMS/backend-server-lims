package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByLotIdOrderByPerformedAtDesc(Long lotId);
    List<InventoryMovement> findByLotIdAndMovementType(Long lotId, String movementType);
    List<InventoryMovement> findByRefEntityAndRefId(String refEntity, Long refId);
}
