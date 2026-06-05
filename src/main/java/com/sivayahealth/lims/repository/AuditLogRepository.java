package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTenantIdAndEntityTypeAndEntityId(Long tenantId, String entityType, Long entityId);
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    Page<AuditLog> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenant.id = :tenantId " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:userId IS NULL OR a.user.id = :userId) " +
           "AND (:from IS NULL OR a.createdAt >= :from) " +
           "AND (:to IS NULL OR a.createdAt <= :to) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("tenantId") Long tenantId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenant.id = :tenantId " +
           "AND a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.createdAt ASC")
    List<AuditLog> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(
            @Param("tenantId") Long tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a WHERE a.tenant.id = :tenantId")
    List<String> findDistinctEntityTypes(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT a.action FROM AuditLog a WHERE a.tenant.id = :tenantId")
    List<String> findDistinctActions(@Param("tenantId") Long tenantId);

    long countByTenantId(Long tenantId);
}
