package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.DocumentControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentControlRepository extends JpaRepository<DocumentControl, Long> {
    Optional<DocumentControl> findByTenantIdAndDocumentCode(Long tenantId, String documentCode);
    List<DocumentControl> findByTenantIdAndStatus(Long tenantId, String status);
    List<DocumentControl> findByTenantId(Long tenantId);

    @Query("SELECT d FROM DocumentControl d WHERE d.tenant.id = :tenantId " +
           "AND d.reviewDueDate IS NOT NULL AND d.reviewDueDate <= :cutoff " +
           "AND d.status = 'PUBLISHED' ORDER BY d.reviewDueDate ASC")
    List<DocumentControl> findDueForReview(@Param("tenantId") Long tenantId, @Param("cutoff") LocalDate cutoff);
}
