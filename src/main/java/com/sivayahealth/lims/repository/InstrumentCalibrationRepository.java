package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentCalibration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

@Repository
public interface InstrumentCalibrationRepository extends JpaRepository<InstrumentCalibration, Long> {
    List<InstrumentCalibration> findByInstrumentId(Long instrumentId);
    List<InstrumentCalibration> findByTenantIdAndStatus(Long tenantId, String status);

    List<InstrumentCalibration> findByTenant_IdAndBranch_IdAndStatus(Long tenantId, Long branchId, String status);

    /** Overdue: calibrationDueDate < today and not yet completed */
    @Query("""
        SELECT c FROM InstrumentCalibration c
        WHERE c.tenant.id = :tenantId
          AND c.branch.id = :branchId
          AND c.calibrationDueDate < :today
          AND c.status NOT IN ('COMPLETED', 'APPROVED', 'ARCHIVED')
        ORDER BY c.calibrationDueDate ASC
        """)
    List<InstrumentCalibration> findOverdueForCalibration(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("today") LocalDate today);

    /** Ready for calibration: instrument AVAILABLE + due date within window */
    @Query("""
        SELECT c FROM InstrumentCalibration c
        WHERE c.tenant.id = :tenantId
          AND c.branch.id = :branchId
          AND c.instrument.status = 'AVAILABLE'
          AND c.calibrationDueDate BETWEEN :from AND :to
          AND c.status = 'SCHEDULED'
        ORDER BY c.calibrationDueDate ASC
        """)
    List<InstrumentCalibration> findReadyForCalibration(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Full lifecycle for a single instrument ordered by creation */
    List<InstrumentCalibration> findByInstrumentIdOrderByCreatedAtAsc(Long instrumentId);
}
