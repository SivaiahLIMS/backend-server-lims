package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InventoryReagentLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReagentLotRepository extends JpaRepository<InventoryReagentLot, Long> {
    List<InventoryReagentLot> findByReagentId(Long reagentId);
    List<InventoryReagentLot> findByReagentIdAndStatus(Long reagentId, String status);
    Optional<InventoryReagentLot> findByLotNumber(String lotNumber);

    @Query("SELECT l FROM InventoryReagentLot l WHERE l.reagent.id = :reagentId AND l.status = 'AVAILABLE' ORDER BY l.expiryDate ASC NULLS LAST")
    List<InventoryReagentLot> findAvailableByReagentFEFO(@Param("reagentId") Long reagentId);

    @Query("SELECT l FROM InventoryReagentLot l WHERE l.reagent.tenant.id = :tenantId AND l.expiryDate <= :date AND l.status = 'AVAILABLE'")
    List<InventoryReagentLot> findExpiringLots(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);
}
