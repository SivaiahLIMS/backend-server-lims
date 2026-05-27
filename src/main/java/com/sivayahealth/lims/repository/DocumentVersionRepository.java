package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocument_Id(Long documentId);
    Optional<DocumentVersion> findByDocument_IdAndVersionNo(Long documentId, int versionNo);
    List<DocumentVersion> findByDocument_IdAndLifecycleState(Long documentId, String lifecycleState);

    /** All versions in a given lifecycle state across the whole tenant */
    @Query("""
        SELECT v FROM DocumentVersion v
        WHERE v.document.tenant.id = :tenantId
          AND v.lifecycleState = :state
        ORDER BY v.uploadedAt DESC
        """)
    List<DocumentVersion> findByTenantAndState(@Param("tenantId") Long tenantId,
                                               @Param("state") String state);

    /** Versions assigned to a specific QC reviewer (reviewedBy set + still UNDER_REVIEW) */
    @Query("""
        SELECT v FROM DocumentVersion v
        WHERE v.document.tenant.id = :tenantId
          AND v.lifecycleState = 'UNDER_REVIEW'
          AND v.reviewedBy.id = :userId
        ORDER BY v.uploadedAt ASC
        """)
    List<DocumentVersion> findAssignedToReviewer(@Param("tenantId") Long tenantId,
                                                 @Param("userId") Long userId);

    /** UNDER_REVIEW versions not yet assigned to any reviewer (unassigned QC queue) */
    @Query("""
        SELECT v FROM DocumentVersion v
        WHERE v.document.tenant.id = :tenantId
          AND v.lifecycleState = 'UNDER_REVIEW'
          AND v.reviewedBy IS NULL
        ORDER BY v.uploadedAt ASC
        """)
    List<DocumentVersion> findUnassignedUnderReview(@Param("tenantId") Long tenantId);

    /** PUBLISHED versions — templates approved by QC, ready for worksheet execution */
    @Query("""
        SELECT v FROM DocumentVersion v
        WHERE v.document.tenant.id = :tenantId
          AND v.lifecycleState = 'PUBLISHED'
        ORDER BY v.document.name ASC
        """)
    List<DocumentVersion> findPublishedForTenant(@Param("tenantId") Long tenantId);
}
