package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.WorksheetExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface WorksheetExecutionRepository extends JpaRepository<WorksheetExecution, Long> {
    List<WorksheetExecution> findByDocumentId(Long documentId);
    List<WorksheetExecution> findByExecutedByIdAndStatus(Long userId, String status);

    /** Pending approval: all SUBMITTED worksheets across the tenant */
    @Query("""
        SELECT w FROM WorksheetExecution w
        WHERE w.document.tenant.id = :tenantId
          AND w.status = 'SUBMITTED'
        ORDER BY w.executedAt ASC
        """)
    List<WorksheetExecution> findPendingApprovalByTenant(@Param("tenantId") Long tenantId);

    /** Rejected worksheets requiring rework */
    @Query("""
        SELECT w FROM WorksheetExecution w
        WHERE w.document.tenant.id = :tenantId
          AND w.status = 'REJECTED'
        ORDER BY w.executedAt DESC
        """)
    List<WorksheetExecution> findRejectedByTenant(@Param("tenantId") Long tenantId);

    /** All worksheets for a tenant (for full review list) */
    @Query("""
        SELECT w FROM WorksheetExecution w
        WHERE w.document.tenant.id = :tenantId
        ORDER BY w.executedAt DESC
        """)
    List<WorksheetExecution> findAllByTenant(@Param("tenantId") Long tenantId);

    /** My pending worksheets (submitted by current user, still SUBMITTED) */
    @Query("""
        SELECT w FROM WorksheetExecution w
        WHERE w.document.tenant.id = :tenantId
          AND w.executedBy.id = :userId
          AND w.status = 'SUBMITTED'
        ORDER BY w.executedAt ASC
        """)
    List<WorksheetExecution> findMyPending(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
