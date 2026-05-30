package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorksheetMasterRepository extends JpaRepository<WorksheetMaster, Long> {

    List<WorksheetMaster> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    List<WorksheetMaster> findByTenantIdAndBranchIdAndStatus(
            Long tenantId, Long branchId, String status);

    List<WorksheetMaster> findByTenantIdAndBranchIdAndIsArchived(
            Long tenantId, Long branchId, boolean isArchived);

    /** Worksheets assigned to a specific analyst in a branch */
    List<WorksheetMaster> findByTenantIdAndBranchIdAndAssignedTo_Id(
            Long tenantId, Long branchId, Long assignedToId);

    /**
     * Rich search with all optional filters, supporting the history screen.
     * All parameters are optional — pass null to skip that filter.
     */
    @Query("""
        SELECT w FROM WorksheetMaster w
        WHERE w.tenant.id = :tenantId
          AND w.branch.id = :branchId
          AND (:status IS NULL OR w.status = :status)
          AND (:isArchived IS NULL OR w.isArchived = :isArchived)
          AND (:productId IS NULL OR w.product.productId = :productId)
          AND (:assignedToId IS NULL OR w.assignedTo.id = :assignedToId)
          AND (:batchNo IS NULL OR w.batchNo = :batchNo)
          AND (:from IS NULL OR w.createdAt >= :from)
          AND (:to IS NULL OR w.createdAt <= :to)
        ORDER BY w.createdAt DESC
        """)
    List<WorksheetMaster> search(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("status") String status,
            @Param("isArchived") Boolean isArchived,
            @Param("productId") Long productId,
            @Param("assignedToId") Long assignedToId,
            @Param("batchNo") String batchNo,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
