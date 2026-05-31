package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetFieldValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorksheetFieldValidationRuleRepository
        extends JpaRepository<WorksheetFieldValidationRule, Long> {

    Optional<WorksheetFieldValidationRule> findBySlot_SlotIdAndActiveTrue(Long slotId);

    @Query("SELECT r FROM WorksheetFieldValidationRule r " +
           "WHERE r.slot.slotId = :slotId ORDER BY r.createdAt DESC")
    List<WorksheetFieldValidationRule> findAllBySlotId(@Param("slotId") Long slotId);
}
